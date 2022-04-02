package contracts.SingleLender

import config.Configs
import contracts.{client, dummyAddress, dummyProver, dummyTxId, ergoClient}
import ergotools.LendServiceTokens
import errors.failedTxException
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, SingleLenderRegister}
import features.lend.boxes.{FundsToAddressBox, LendServiceBox, SingleLenderLendBox, SingleLenderRepaymentBox}
import features.lend.contracts.proxyContracts.LendProxyContractService
import org.ergoplatform.appkit.{Address, ErgoContract, OutBox, Parameters, SignedTransaction, UnsignedTransaction}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sigmastate.lang.exceptions.InterpreterException
import special.collection.Coll

import scala.collection.JavaConverters.seqAsJavaListConverter

class SLProxyContractsSpec extends AnyWordSpec with Matchers {
  val serviceBox: LendServiceBox = buildGenesisServiceBox()

  client.setClient()
  val lendProxyContractService = new LendProxyContractService(client)

  "LendBox: Instantiate" when {
    /**
     * 1. Not enough ergs in Proxy Contract
     * 2. Just enough ergs in Proxy Contract
     */
    "instantiating a box" should {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val inputServiceBox = serviceBox
            .getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          val lendCreationProxyContract: ErgoContract =
            lendProxyContractService.getLendCreateProxyContract(
              pk = dummyAddress.toString,
              deadlineHeight = ctx.getHeight + deadlineHeightLength,
              goal = goal,
              interestRate = interestRate,
              repaymentHeightLength = repaymentHeightLength
            )

          val inputProxyContract = txB.outBoxBuilder()
            .contract(lendCreationProxyContract)
            .value(Parameters.MinFee * 3 + Configs.serviceFee)
            .build()
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputServiceBox = serviceBox.createLend().getOutputServiceBox(ctx, txB)
          val fundingInfoRegister = new FundingInfoRegister(
            fundingGoal = goal,
            deadlineHeight = ctx.getHeight + deadlineHeightLength,
            interestRatePercent = interestRate,
            repaymentHeightLength = repaymentHeightLength
          )
          val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
            projectName = loanName,
            description = loanDescription,
          )
          val borrowerRegister = new BorrowerRegister(dummyAddress.toString)
          val lendBox = new SingleLenderLendBox(
            value = Parameters.MinFee,
            fundingInfoRegister = fundingInfoRegister,
            lendingProjectDetailsRegister = lendingProjectDetailsRegister,
            borrowerRegister = borrowerRegister,
            singleLenderRegister = SingleLenderRegister.emptyRegister
          ).getOutputBox(ctx, txB)

          val outputOwnerFeeBox = new FundsToAddressBox(
            value = Configs.serviceFee,
            address = Configs.serviceOwner).getOutputBox(ctx, txB)

          val tx = txB.boxesToSpend(Seq(inputServiceBox, inputProxyContract).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputServiceBox, lendBox, outputOwnerFeeBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          var signed: SignedTransaction = null
          try {
            signed = dummyProver.sign(tx)

            // Checks service box to have one less lend token
            "returns service box with one less lend token" in {
              val outputServiceBox = signed.getOutputsToSpend.get(0)
              assert(outputServiceBox.getTokens.get(0).getId == LendServiceTokens.nft)
              assert(outputServiceBox.getTokens.get(0).getValue == 1)
              assert(outputServiceBox.getTokens.get(1).getId == LendServiceTokens.lendToken)
              assert(outputServiceBox.getTokens.get(1).getValue == (inputServiceBox.getTokens.get(1).getValue - 1))
              assert(outputServiceBox.getTokens.get(2).getId == LendServiceTokens.repaymentToken)
              assert(outputServiceBox.getTokens.get(2).getValue == inputServiceBox.getTokens.get(2).getValue)
            }

            "successfully create a lendbox" in {
              val outputLendBox = signed.getOutputsToSpend.get(1)
              assert(outputLendBox.getValue == Parameters.MinFee, "The instantiated lendBox value is not minimum")
              assert(outputLendBox.getTokens.get(0).getValue == 1, "The instantiated lendBox lend token is not 1")
              assert(outputLendBox.getTokens.get(0).getId == LendServiceTokens.lendToken, "The instantiated lendBox token is incorrect")
            }

            "send fee to owner" in {
              val outputFeeBox = signed.getOutputsToSpend.get(2)
              val outputAddress = Configs.addressEncoder.fromProposition(outputFeeBox.getErgoTree)
              assert(outputFeeBox.getValue >= Configs.serviceFee, "Service Fee: Service fee is incorrect")
              assert(outputAddress.get == Configs.serviceOwner.getErgoAddress, "Service Fee: Output Service Fee sent to wrong box")
            }
          } catch {
            case e: Exception => {
              "should pass" in {
                fail("transaction can't be signed" + e.printStackTrace())
              }
            }
          }
        }
      }
    }

    // Pays to ErgoLend
    "refunding an instantiate lend proxy contract" should {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val lendCreationProxyContract: ErgoContract =
            lendProxyContractService.getLendCreateProxyContract(
              pk = dummyAddress.toString,
              deadlineHeight = ctx.getHeight + deadlineHeightLength,
              goal = goal,
              interestRate = interestRate,
              repaymentHeightLength = repaymentHeightLength
            )

          val inputProxyContract = txB.outBoxBuilder()
            .contract(lendCreationProxyContract)
            .value(Parameters.MinFee * 3 + Configs.serviceFee)
            .build()
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val refundToBorrower = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, dummyAddress.toString)
            .getOutputBox(ctx, txB)

          val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
            .fee(Parameters.MinFee)
            .outputs(refundToBorrower)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          var signed: SignedTransaction = null
          signed = dummyProver.sign(tx)
        }
      }
    }

    "refunding a instantiate lend proxy contract to other than borrower" should {
      "fail" in {

      ergoClient.execute {
        ctx => {
            val txB = ctx.newTxBuilder()

            // Input Boxes
            val lendCreationProxyContract: ErgoContract =
              lendProxyContractService.getLendCreateProxyContract(
                pk = dummyAddress.toString,
                deadlineHeight = ctx.getHeight + deadlineHeightLength,
                goal = goal,
                interestRate = interestRate,
                repaymentHeightLength = repaymentHeightLength
              )

            val inputProxyContract = txB.outBoxBuilder()
              .contract(lendCreationProxyContract)
              .value(Parameters.MinFee * 3 + Configs.serviceFee)
              .build()
              .convertToInputWith(dummyTxId, 0)

            // Output Boxes
            val refundToBorrower = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, Configs.serviceOwner.toString)
              .getOutputBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
              .fee(Parameters.MinFee)
              .outputs(refundToBorrower)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            assertThrows[InterpreterException] {
              dummyProver.sign(tx)
            }
          }
        }
      }
    }
  }

  "LendBox: Funding" when {
    "funding a lend box" can {
      "fund successfully" should {
        val fundAmount = goal + Parameters.MinFee * 2
        val signedTx: SignedTransaction = fundLendBoxTx(fundAmount, dummyAddress)
        val outputLendBox = signedTx.getOutputsToSpend.get(0)
        val r4 = outputLendBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray
        val r7 = outputLendBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray
        val fundingInfoRegister = new FundingInfoRegister(r4)
        val lendingInfoRegister = new SingleLenderRegister(r7)

        /**
         * Check For:
         * 1. If box is funded
         * 2. If Tx went through
         */
        "Funded Amount is same as funding goal" in {
          assert(outputLendBox.getValue >= fundingInfoRegister.fundingGoal)
          assert(dummyAddress.toString == lendingInfoRegister.lendersAddress)
        }
      }

      "reabsorb a lend box before Due Date" should {
        ergoClient.execute {
          ctx => {
            val tx = refundLendBoxTx()
            "should fail as due date is not there yet" in {
              assertThrows[Exception] {
                dummyProver.sign(tx)
              }
            }
          }
        }
      }

      "reabsorb a minimum lend box after Due Date" should {
        ergoClient.execute {
          ctx => {
            val tx = refundLendBoxTx(deadlineHeight = -100)
            "should pass as funding has expired" in {
              dummyProver.sign(tx)
            }
          }
        }
      }

      "reabsorb a funded lend box after Due Date" in {
        ergoClient.execute {
          ctx => {
            val tx = refundLendBoxTx(deadlineHeight = -100, funded = true)
            assertThrows[Exception] {
              dummyProver.sign(tx)
            }
          }
        }
      }

      "over-funding a lend box" should {
        val fundAmount = goal * 2 + Parameters.MinFee * 2
        val tx = overfundLendBoxTx(fundAmount)

        val signedTx = dummyProver.sign(tx)
        "Funded Amount is same as funding goal" in {
          assert(signedTx.getOutputsToSpend.get(0).getValue == goal + Parameters.MinFee * 2)
        }
      }

      "over-funding a lend box, but getting hacked" should {
        val fundAmount = goal * 2 + Parameters.MinFee * 2
        val tx = overfundLendBoxTx(fundAmount, hacked = true)

        "fail" in {
          assertThrows[InterpreterException] {
            dummyProver.sign(tx)
          }
        }
      }
    }

    "dealing with fundLendContract" can {
      "refund" should {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            val wrappedInputLendBox = createWrappedLendBox()
            val inputLendBox = wrappedInputLendBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            val fundLendProxyContract: ErgoContract =
              lendProxyContractService.getFundLendBoxProxyContract(
                lendId = inputLendBox.getId.toString,
                lenderAddress = dummyAddress.toString
              )

            val inputProxyContract = txB.outBoxBuilder()
              .contract(fundLendProxyContract)
              .value(1e9.toLong)
              .build()
              .convertToInputWith(dummyTxId, 0)

            // OutBox
            val refundToLender = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, dummyAddress.toString)
              .getOutputBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
              .fee(Parameters.MinFee)
              .outputs(refundToLender)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            val signedTx = dummyProver.sign(tx)
            "successfully" in {
              assert(Configs.addressEncoder.fromProposition(signedTx.getOutputsToSpend.get(0).getErgoTree).get
                == dummyAddress.getErgoAddress)
            }
          }
        }

        "refund to different lender" should {
          ergoClient.execute {
            ctx => {
              val txB = ctx.newTxBuilder()

              val wrappedInputLendBox = createWrappedLendBox()
              val inputLendBox = wrappedInputLendBox.getOutputBox(ctx, txB)
                .convertToInputWith(dummyTxId, 0)

              val fundLendProxyContract: ErgoContract =
                lendProxyContractService.getFundLendBoxProxyContract(
                  lendId = inputLendBox.getId.toString,
                  lenderAddress = dummyAddress.toString
                )

              val inputProxyContract = txB.outBoxBuilder()
                .contract(fundLendProxyContract)
                .value(1e9.toLong)
                .build()
                .convertToInputWith(dummyTxId, 0)

              // OutBox
              val refundToLender = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, Configs.serviceOwner.toString)
                .getOutputBox(ctx, txB)

              val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
                .fee(Parameters.MinFee)
                .outputs(refundToLender)
                .sendChangeTo(dummyAddress.getErgoAddress)
                .build()

              val signedTx = intercept[InterpreterException] {
                dummyProver.sign(tx)
              }

              "fail" in {
                assert(signedTx.getMessage() == "Script reduced to false")
              }
            }
          }
        }
      }
    }
  }

  "Fund Loan Successfully" when {
    "fund successful" should {
      "hack to repay to other than borrower fails" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // box funded
            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedLendBox = createWrappedLendBox().fundBox(dummyAddress.toString)

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputLendBox = wrappedLendBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // outputs
            val outputServiceBox = wrappedServiceBox.fundedLend()
              .getOutputServiceBox(ctx, txB)
            val repaymentBox = new SingleLenderRepaymentBox(
              wrappedLendBox,
              ctx.getHeight + 10
            ).getOutputBox(ctx, txB)
            val borrowerFunds = new FundsToAddressBox(
              wrappedLendBox.fundingInfoRegister.fundingGoal,
              Configs.serviceOwner.toString)
              .getOutputBox(ctx, txB)


            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(
                outputServiceBox,
                repaymentBox,
                borrowerFunds
              )
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            assertThrows[InterpreterException] {
              dummyProver.sign(tx)
            }
          }
        }
      }

      "convert to repayment box, repaid to borrower succeed" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // box funded
            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedLendBox = createWrappedLendBox().fundBox(dummyAddress.toString)

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputLendBox = wrappedLendBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // outputs
            val outputServiceBox = wrappedServiceBox.fundedLend()
              .getOutputServiceBox(ctx, txB)
            val repaymentBox = new SingleLenderRepaymentBox(
              wrappedLendBox,
              ctx.getHeight + 10
            ).getOutputBox(ctx, txB)
            val borrowerFunds = new FundsToAddressBox(
              wrappedLendBox.fundingInfoRegister.fundingGoal,
              wrappedLendBox.borrowerRegister.borrowersAddress)
              .getOutputBox(ctx, txB)


            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(
                outputServiceBox,
                repaymentBox,
                borrowerFunds
              )
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            dummyProver.sign(tx)
          }
        }
      }

      "convert a passed deadline fundbox succeeds (if converted to repayment and borrower)" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // box funded
            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedLendBox = createWrappedLendBox(deadlineHeightLength = ctx.getHeight - 100).fundBox(dummyAddress.toString)

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputLendBox = wrappedLendBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // outputs
            val outputServiceBox = wrappedServiceBox.fundedLend()
              .getOutputServiceBox(ctx, txB)
            val repaymentBox = new SingleLenderRepaymentBox(
              wrappedLendBox,
              ctx.getHeight + 10
            ).getOutputBox(ctx, txB)
            val borrowerFunds = new FundsToAddressBox(
              wrappedLendBox.fundingInfoRegister.fundingGoal,
              wrappedLendBox.borrowerRegister.borrowersAddress)
              .getOutputBox(ctx, txB)


            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(
                outputServiceBox,
                repaymentBox,
                borrowerFunds
              )
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            dummyProver.sign(tx)
          }
        }
      }

      "no repayment box fails" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // box funded
            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedLendBox = createWrappedLendBox().fundBox(dummyAddress.toString)

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputLendBox = wrappedLendBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // outputs
            val outputServiceBox = wrappedServiceBox.refundLend()
              .getOutputServiceBox(ctx, txB)
            val borrowerFunds = new FundsToAddressBox(
              wrappedLendBox.fundingInfoRegister.fundingGoal + Parameters.MinFee,
              Configs.serviceOwner.toString)
              .getOutputBox(ctx, txB)


            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(
                outputServiceBox,
                borrowerFunds
              )
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            assertThrows[Exception] {
              dummyProver.sign(tx)
            }
          }
        }
      }
    }
  }

  "RepaymentBox" when {
    "funding a repayment box" can {
      "fund full amount to repayment box" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // Input Boxes
            val wrappedInputRepaymentBox = createWrappedRepaymentBox()

            val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            val fundLendProxyContract: ErgoContract =
              lendProxyContractService.getFundRepaymentBoxProxyContract(
                repaymentBoxId = inputRepaymentBox.getId.toString,
                funderPk = dummyAddress.toString
              )

            val inputProxyContract = txB.outBoxBuilder()
              .contract(fundLendProxyContract)
              .value(wrappedInputRepaymentBox.getFullFundAmount)
              .build()
              .convertToInputWith(dummyTxId, 0)

            // Output Boxes
            val outputRepaymentBox = wrappedInputRepaymentBox.fundedBox().getOutputBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputRepaymentBox, inputProxyContract).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputRepaymentBox)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            dummyProver.sign(tx)
          }
        }
      }

      "fund partial amount to repayment box" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            // Input Boxes
            val wrappedInputRepaymentBox = createWrappedRepaymentBox()

            val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            val fundLendProxyContract: ErgoContract =
              lendProxyContractService.getFundRepaymentBoxProxyContract(
                repaymentBoxId = inputRepaymentBox.getId.toString,
                funderPk = dummyAddress.toString
              )

            val fundAmount = 0.5e9.toLong

            val inputProxyContract = txB.outBoxBuilder()
              .contract(fundLendProxyContract)
              .value(fundAmount + Parameters.MinFee)
              .build()
              .convertToInputWith(dummyTxId, 0)

            // Output Boxes
            val outputRepaymentBox = wrappedInputRepaymentBox.fundBox(fundAmount).getOutputBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputRepaymentBox, inputProxyContract).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputRepaymentBox)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            dummyProver.sign(tx)
          }
        }
      }

      "over-fund amount to repayment box" should {
        "without returning overpayment to funder" in {
          ergoClient.execute {
            ctx => {
              val txB = ctx.newTxBuilder()

              // Input Boxes
              val wrappedInputRepaymentBox = createWrappedRepaymentBox()

              val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
                .convertToInputWith(dummyTxId, 0)

              val fundLendProxyContract: ErgoContract =
                lendProxyContractService.getFundRepaymentBoxProxyContract(
                  repaymentBoxId = inputRepaymentBox.getId.toString,
                  funderPk = dummyAddress.toString
                )

              val fundAmount = wrappedInputRepaymentBox.getFullFundAmount * 2

              val inputProxyContract = txB.outBoxBuilder()
                .contract(fundLendProxyContract)
                .value(fundAmount + Parameters.MinFee)
                .build()
                .convertToInputWith(dummyTxId, 0)

              // Output Boxes
              val outputRepaymentBox = wrappedInputRepaymentBox.fundBox(fundAmount).getOutputBox(ctx, txB)

              val tx = txB.boxesToSpend(Seq(inputRepaymentBox, inputProxyContract).asJava)
                .fee(Parameters.MinFee)
                .outputs(outputRepaymentBox)
                .sendChangeTo(dummyAddress.getErgoAddress)
                .build()

              assertThrows[InterpreterException] {
                dummyProver.sign(tx)
              }
            }
          }
        }

        "returning overpayment to Someone else" in {
          ergoClient.execute {
            ctx => {
              val txB = ctx.newTxBuilder()

              // Input Boxes
              val wrappedInputRepaymentBox = createWrappedRepaymentBox()

              val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
                .convertToInputWith(dummyTxId, 0)

              val fundLendProxyContract: ErgoContract =
                lendProxyContractService.getFundRepaymentBoxProxyContract(
                  repaymentBoxId = inputRepaymentBox.getId.toString,
                  funderPk = dummyAddress.toString
                )

              val fundAmount = wrappedInputRepaymentBox.getFullFundAmount * 2

              val inputProxyContract = txB.outBoxBuilder()
                .contract(fundLendProxyContract)
                .value(fundAmount + Parameters.MinFee)
                .build()
                .convertToInputWith(dummyTxId, 0)

              // Output Boxes
              val outputRepaymentBox = wrappedInputRepaymentBox.fundedBox().getOutputBox(ctx, txB)
              val fundBackToFunderBox = new FundsToAddressBox(fundAmount + Parameters.MinFee - outputRepaymentBox.getValue, Configs.serviceOwner.toString)
                .getOutputBox(ctx, txB)

              val totalInputVal = inputRepaymentBox.getValue + inputProxyContract.getValue
              val totalOutputVal = outputRepaymentBox.getValue + fundBackToFunderBox.getValue
              assert((totalInputVal-totalOutputVal) == Parameters.MinFee, "Input and Output Value is inbalance")

              val tx = txB.boxesToSpend(Seq(inputRepaymentBox, inputProxyContract).asJava)
                .fee(Parameters.MinFee)
                .outputs(outputRepaymentBox, fundBackToFunderBox)
                .sendChangeTo(dummyAddress.getErgoAddress)
                .build()

              assertThrows[InterpreterException] {
                dummyProver.sign(tx)
              }
            }
          }
        }

        "returning overpayment to funder" in {
          ergoClient.execute {
            ctx => {
              val txB = ctx.newTxBuilder()

              // Input Boxes
              val wrappedInputRepaymentBox = createWrappedRepaymentBox()

              val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
                .convertToInputWith(dummyTxId, 0)

              val fundLendProxyContract: ErgoContract =
                lendProxyContractService.getFundRepaymentBoxProxyContract(
                  repaymentBoxId = inputRepaymentBox.getId.toString,
                  funderPk = dummyAddress.toString
                )

              val fundAmount = wrappedInputRepaymentBox.getFullFundAmount * 2

              val inputProxyContract = txB.outBoxBuilder()
                .contract(fundLendProxyContract)
                .value(fundAmount + Parameters.MinFee)
                .build()
                .convertToInputWith(dummyTxId, 0)

              // Output Boxes
              val outputRepaymentBox = wrappedInputRepaymentBox.fundedBox().getOutputBox(ctx, txB)
              val fundBackToFunderBox = new FundsToAddressBox(fundAmount + Parameters.MinFee - outputRepaymentBox.getValue, dummyAddress.toString)
                .getOutputBox(ctx, txB)

              val totalInputVal = inputRepaymentBox.getValue + inputProxyContract.getValue
              val totalOutputVal = outputRepaymentBox.getValue + fundBackToFunderBox.getValue
              assert((totalInputVal-totalOutputVal) == Parameters.MinFee, "Input and Output Value is inbalance")

              val tx = txB.boxesToSpend(Seq(inputRepaymentBox, inputProxyContract).asJava)
                .fee(Parameters.MinFee)
                .outputs(outputRepaymentBox, fundBackToFunderBox)
                .sendChangeTo(dummyAddress.getErgoAddress)
                .build()

              dummyProver.sign(tx)
            }
          }
        }
      }
    }

    "dealing with fund repayment proxy contract" can {
      "refund" should {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            val wrappedInputRepaymentBox = createWrappedRepaymentBox()
            val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            val fundLendProxyContract: ErgoContract =
              lendProxyContractService.getFundRepaymentBoxProxyContract(
                repaymentBoxId = inputRepaymentBox.getId.toString,
                funderPk = dummyAddress.toString
              )

            val inputProxyContract = txB.outBoxBuilder()
              .contract(fundLendProxyContract)
              .value(1e9.toLong)
              .build()
              .convertToInputWith(dummyTxId, 0)

            // OutBox
            val refundToFunder = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, dummyAddress.toString)
              .getOutputBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
              .fee(Parameters.MinFee)
              .outputs(refundToFunder)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            val signedTx = dummyProver.sign(tx)
            "successfully" in {
              assert(Configs.addressEncoder.fromProposition(signedTx.getOutputsToSpend.get(0).getErgoTree).get
                == dummyAddress.getErgoAddress)
            }
          }
        }

        "refund to different lender" should {
          "fail" in {
            ergoClient.execute {
              ctx => {
                val txB = ctx.newTxBuilder()

                val wrappedInputRepaymentBox = createWrappedRepaymentBox()
                val inputRepaymentBox = wrappedInputRepaymentBox.getOutputBox(ctx, txB)
                  .convertToInputWith(dummyTxId, 0)

                val fundRepaymentProxyContract: ErgoContract =
                  lendProxyContractService.getFundRepaymentBoxProxyContract(
                    repaymentBoxId = inputRepaymentBox.getId.toString,
                    funderPk = dummyAddress.toString
                  )

                val inputProxyContract = txB.outBoxBuilder()
                  .contract(fundRepaymentProxyContract)
                  .value(1e9.toLong)
                  .build()
                  .convertToInputWith(dummyTxId, 0)

                // OutBox
                val refundToLender = new FundsToAddressBox(inputProxyContract.getValue - Parameters.MinFee, Configs.serviceOwner.toString)
                  .getOutputBox(ctx, txB)

                val tx = txB.boxesToSpend(Seq(inputProxyContract).asJava)
                  .fee(Parameters.MinFee)
                  .outputs(refundToLender)
                  .sendChangeTo(dummyAddress.getErgoAddress)
                  .build()

                assertThrows[InterpreterException] {
                  dummyProver.sign(tx)
                }
              }
            }
          }
        }
      }
    }

    "consume semi-funded" should {
      "fail if deadline not passed" in {
        val unsignedTx: UnsignedTransaction = consumeRepaymentTx(funded = false)

        assertThrows[InterpreterException] {
          dummyProver.sign(unsignedTx)
        }
      }
    }

    "consume successfully funded Repayment" in {
      val unsignedTx: UnsignedTransaction = consumeRepaymentTx(funded = true)

      dummyProver.sign(unsignedTx)
    }

    "semi-funded but defaulted" should {
      "still repayable to lender" in {
        val tx = defaultedSemiFundedTx(isHackerHackRepaidBox = false)
        dummyProver.sign(tx)
      }

      "hacker hack repayment box" in {
        val tx = defaultedSemiFundedTx(isHackerHackRepaidBox = true)
        assertThrows[InterpreterException] {
          dummyProver.sign(tx)
        }
      }

      def defaultedSemiFundedTx(isHackerHackRepaidBox: Boolean) : UnsignedTransaction = {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedRepaymentBox = createRawWrappedRepaymentBox(interestRate = 0, fundedRepaymentHeight = (ctx.getHeight - 1000)).fundBox(0.5e9.toLong)

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputRepaymentBox = wrappedRepaymentBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // Output Boxes
            val outputServiceBox = wrappedServiceBox.incrementRepaymentToken().getOutputServiceBox(ctx, txB)
            val lendersBox = new FundsToAddressBox(
              wrappedRepaymentBox.value - Parameters.MinFee,
              if (!isHackerHackRepaidBox) {dummyAddress.toString} else {Configs.serviceOwner.toString})
              .getOutputBox(ctx, txB)

            val totalInputVal = inputServiceBox.getValue + inputRepaymentBox.getValue
            val totalOutputVal = outputServiceBox.getValue + lendersBox.getValue

            assert(totalInputVal - totalOutputVal == Parameters.MinFee, "Input, output value inbalance")
            assert(ctx.getHeight > wrappedRepaymentBox.repaymentDetailsRegister.repaymentHeightGoal)

            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputServiceBox, lendersBox)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            tx
          }
        }
      }
    }

    "consume defaulted and not repaid at all" can {
      "fail" in {
        ergoClient.execute {
          ctx => {
            val txB = ctx.newTxBuilder()

            val wrappedServiceBox = buildGenesisServiceBox()
            val wrappedRepaymentBox = createRawWrappedRepaymentBox(interestRate = 0, fundedRepaymentHeight = (ctx.getHeight - 1000))

            val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)
            val inputRepaymentBox = wrappedRepaymentBox.getOutputBox(ctx, txB)
              .convertToInputWith(dummyTxId, 0)

            // Output Boxes
            val outputServiceBox = wrappedServiceBox.incrementRepaymentToken().getOutputServiceBox(ctx, txB)

            val tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputServiceBox)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            assertThrows[Exception] {
              dummyProver.sign(tx)
            }
          }
        }
      }
    }

    "profit sharing less than min box" in {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val wrappedServiceBox = buildGenesisServiceBox()
          var wrappedRepaymentBox = createWrappedRepaymentBox(fundingGoal = 0.01e9.toLong, interestRate = 1)
            .fundedBox()

          wrappedRepaymentBox = wrappedRepaymentBox.fundedBox()

          val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)
          val inputRepaymentBox = wrappedRepaymentBox.getOutputBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputBoxes = wrappedServiceBox.consumeRepaymentBox(
            repaymentBox = wrappedRepaymentBox,
            ctx,
            txB).asJava

          val totalInputVal = inputServiceBox.getValue + inputRepaymentBox.getValue
          val totalOutputVal = outputBoxes.get(0).getValue + outputBoxes.get(1).getValue
          val outputServiceBox = outputBoxes.get(0).convertToInputWith(dummyTxId, 0)
          val outputLenderBox = outputBoxes.get(1).convertToInputWith(dummyTxId, 0)

          assert(totalInputVal - totalOutputVal == Parameters.MinFee, "Input, output value inbalance")
          assert(inputServiceBox.getTokens.get(2).getValue == outputServiceBox.getTokens.get(2).getValue - 1)
          assert(outputLenderBox.getValue >= wrappedRepaymentBox.fundingInfoRegister.fundingGoal)
          assert(Configs.addressEncoder.fromProposition(outputLenderBox.getErgoTree).get == dummyAddress.getErgoAddress)
          assert(inputRepaymentBox.getValue >= wrappedRepaymentBox.repaymentDetailsRegister.repaymentAmount)

          val tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputBoxes.get(0), outputBoxes.get(1))
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          dummyProver.sign(tx)
        }
      }
    }

    "interest rates is at 0%" in {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val wrappedServiceBox = buildGenesisServiceBox()
          var wrappedRepaymentBox = createWrappedRepaymentBox(interestRate = 0).fundBox(0.3e9.toLong)

          wrappedRepaymentBox = wrappedRepaymentBox.fundedBox()

          val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)
          val inputRepaymentBox = wrappedRepaymentBox.getOutputBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputBoxes = wrappedServiceBox.consumeRepaymentBox(
            repaymentBox = wrappedRepaymentBox,
            ctx,
            txB).asJava

          val totalInputVal = inputServiceBox.getValue + inputRepaymentBox.getValue
          val totalOutputVal = outputBoxes.get(0).getValue + outputBoxes.get(1).getValue
          val outputServiceBox = outputBoxes.get(0).convertToInputWith(dummyTxId, 0)
          val outputLenderBox = outputBoxes.get(1).convertToInputWith(dummyTxId, 0)

          assert(totalInputVal - totalOutputVal == Parameters.MinFee, "Input, output value inbalance")
          assert(inputServiceBox.getTokens.get(2).getValue == outputServiceBox.getTokens.get(2).getValue - 1)
          assert(outputLenderBox.getValue >= wrappedRepaymentBox.fundingInfoRegister.fundingGoal)
          assert(Configs.addressEncoder.fromProposition(outputLenderBox.getErgoTree).get == dummyAddress.getErgoAddress)
          assert(inputRepaymentBox.getValue >= wrappedRepaymentBox.repaymentDetailsRegister.repaymentAmount)

          val tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputBoxes.get(0), outputBoxes.get(1))
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          dummyProver.sign(tx)
        }
      }
    }

    def consumeRepaymentTx(funded: Boolean): UnsignedTransaction = {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val wrappedServiceBox = buildGenesisServiceBox()
          var wrappedRepaymentBox = createWrappedRepaymentBox().fundBox(0.3e9.toLong)

          if (funded) {
            wrappedRepaymentBox = wrappedRepaymentBox.fundedBox()
          }

          val inputServiceBox = wrappedServiceBox.getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)
          val inputRepaymentBox = wrappedRepaymentBox.getOutputBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputBoxes: java.util.List[OutBox] = wrappedServiceBox.consumeRepaymentBox(
            repaymentBox = wrappedRepaymentBox,
            ctx,
            txB).asJava

          var tx: UnsignedTransaction = null
          if (outputBoxes.size() == 3) {
            tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputBoxes.get(0), outputBoxes.get(1), outputBoxes.get(2))
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()
          } else {
            tx = txB.boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(outputBoxes.get(0), outputBoxes.get(1))
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()
          }

          tx
        }
      }
    }
  }


  // <editor-fold desc="Functions for Tx">
  def refundLendBoxTx(deadlineHeight: Long = 100,
                      funded: Boolean = false): UnsignedTransaction = {
    ergoClient.execute {
      ctx => {
        val txB = ctx.newTxBuilder()

        // Input Boxes
        val serviceBox = buildGenesisServiceBox()
          .negateLendToken()

        val inputServiceBox = serviceBox
          .getOutputServiceBox(ctx, txB)
          .convertToInputWith(dummyTxId, 0)
        val wrappedLendBox = if (funded) {
          createWrappedLendBox(deadlineHeightLength = deadlineHeight)
            .fundBox(dummyAddress.toString)
        } else {
          createWrappedLendBox(deadlineHeightLength = deadlineHeight)
        }

        val inputLendBox =
          wrappedLendBox.getOutputBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

        // Output Boxes
        val outputServiceBox = serviceBox.refundLend().getOutputServiceBox(ctx, txB)

        var tx: UnsignedTransaction = null
        if (funded) {
          val outputLenderBox =
            new FundsToAddressBox(
              wrappedLendBox.fundingInfoRegister.fundingGoal,
              wrappedLendBox.singleLenderRegister.lendersAddress)
              .getOutputBox(ctx, txB)

          tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputServiceBox, outputLenderBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()
        } else {
          tx = txB.boxesToSpend(Seq(inputServiceBox, inputLendBox).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputServiceBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()
        }

        tx
      }
    }
  }

  def overfundLendBoxTx(fundAmount: Long, hacked: Boolean = false): UnsignedTransaction = {
    ergoClient.execute {
      ctx => {
        val txB = ctx.newTxBuilder()

        // Input Box
        val wrappedInputLendBox = createWrappedLendBox()
        val inputLendBox = wrappedInputLendBox.getOutputBox(ctx, txB)
          .convertToInputWith(dummyTxId, 0)

        val fundLendProxyContract: ErgoContract =
          lendProxyContractService.getFundLendBoxProxyContract(
            lendId = inputLendBox.getId.toString,
            lenderAddress = dummyAddress.toString
          )

        val inputProxyContract = txB.outBoxBuilder()
          .contract(fundLendProxyContract)
          .value(fundAmount)
          .build()
          .convertToInputWith(dummyTxId, 0)

        // Output Boxes
        val outputLendBox = wrappedInputLendBox.fundBox(dummyAddress.toString)
          .getOutputBox(ctx, txB)

        val fundsToAddressBox = new FundsToAddressBox(fundAmount - outputLendBox.getValue, if (hacked) Configs.serviceOwner.toString else dummyAddress.toString)
          .getOutputBox(ctx, txB)

        val tx = txB.boxesToSpend(Seq(inputLendBox, inputProxyContract).asJava)
          .fee(Parameters.MinFee)
          .outputs(outputLendBox, fundsToAddressBox)
          .sendChangeTo(dummyAddress.getErgoAddress)
          .build()

        tx
      }
    }
  }

  def fundLendBoxTx(fundAmount: Long, lenderAddress: Address): SignedTransaction = {
    ergoClient.execute {
      ctx => {
        val txB = ctx.newTxBuilder()

        // Input Box
        val wrappedInputLendBox = createWrappedLendBox()
        val inputLendBox = wrappedInputLendBox.getOutputBox(ctx, txB)
          .convertToInputWith(dummyTxId, 0)

        val lendCreationProxyContract: ErgoContract =
          lendProxyContractService.getFundLendBoxProxyContract(
            lendId = inputLendBox.getId.toString,
            lenderAddress = lenderAddress.toString
          )

        val inputProxyContract = txB.outBoxBuilder()
          .contract(lendCreationProxyContract)
          .value(fundAmount)
          .build()
          .convertToInputWith(dummyTxId, 0)

        // Output Boxes
        val outputLendBox = wrappedInputLendBox.fundBox(lenderAddress.toString)
          .getOutputBox(ctx, txB)

        val tx = txB.boxesToSpend(Seq(inputLendBox, inputProxyContract).asJava)
          .fee(Parameters.MinFee)
          .outputs(outputLendBox)
          .sendChangeTo(lenderAddress.getErgoAddress)
          .build()

        val signedTransaction = dummyProver.sign(tx)

        signedTransaction
      }
    }
  }

  // </editor-fold>
}

