package lendcore.chain.explorer

import org.ergoplatform.appkit.{Address, ErgoId}
import org.ergoplatform.explorer.client.model._

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsScalaMapConverter}

object Models {

  abstract class ExplorerConversion[E, C] {
    def fromExplorer(exp: E): C

    def fromExplorerList(exp: java.util.List[E]): Seq[C] = {
      exp.asScala.map(fromExplorer).toSeq
    }

    def fromOption(opt: Option[E]): Option[C] = {
      if(opt.isDefined){
        Some(fromExplorer(opt.get))
      }else{
        None
      }
    }

    def fromOptionSeq(optSeq: Option[Seq[E]]): Option[Seq[C]] = {
      if(optSeq.isDefined){
        Some(optSeq.get.map(fromExplorer))
      }else{
        None
      }
    }
  }
  case class RegisterData(serialized: String, sigmaType: String, renderedValue: String){
    override def toString: String = s"Reg[$sigmaType]($renderedValue)"
  }

  case class Registers(R4: Option[RegisterData], R5: Option[RegisterData], R6: Option[RegisterData],
                       R7: Option[RegisterData], R8: Option[RegisterData], R9: Option[RegisterData]){
    override def toString: String = {
      s"      (" +
        s"\n        R4 -> ${R4.toString}, R5 -> ${R5.toString}, R6 -> ${R6.toString}" +
        s"\n        R7 -> ${R7.toString}, R8 -> ${R8.toString}, R9 -> ${R9.toString}" +
        "\n      )"

    }
  }

  case class TokenData(id: ErgoId, amount: Long, name: String, displayDecimals: Int){
    override def toString: String = s"      Token(${id.toString.take(6)}...${id.toString.takeRight(6)} -> $amount | $name)"
  }
  case class ExtendedTokenData(id: ErgoId, amount: Long, index: Int, name: String, displayDecimals: Int, tokenType: String){
    override def toString: String = s"      Token[$index](${id.toString.take(6)}...${id.toString.takeRight(6)} -> $amount | $name)"
  }

  case class Output(id: ErgoId, txId: ErgoId, blockId: ErgoId, value: Long, outputIndex: Int, heightCreated: Int,
                    heightSettled: Int, ergoTreeHex: String, address: Address, assets: Seq[ExtendedTokenData],
                    registers: Registers, spendingTxId: Option[ErgoId], isOnMainChain: Boolean){
    override def toString: String = {
      s"""Output[$outputIndex](
         |    id: ${id.toString}
         |    txId: ${txId.toString}
         |    blockId: ${Helpers.trunc(blockId.toString)}
         |    value: $value | ${Helpers.nanoErgToErg(value)} ERG
         |    heightCreated: $heightCreated
         |    heightSettled: $heightSettled
         |    ergoTree: ${Helpers.trunc(ergoTreeHex)}
         |    address: ${Helpers.trunc(address.toString, 12)}
         |    assets: \n${assets.mkString("\n")}
         |    registers: \n$registers
         |    spendingTx: $spendingTxId
         |    mainChain: $isOnMainChain
         |""".stripMargin
    }
  }

  case class Input(id: ErgoId, creationTxId: ErgoId, creationBlockId: ErgoId, value: Long, inputIndex: Int, spendingProof: String,
                   outputIndex: Int, ergoTreeHex: String, address: Address, assets: Seq[ExtendedTokenData],
                   registers: Registers)

  case class DataInput(id: ErgoId, creationTxId: ErgoId, creationBlockId: ErgoId, value: Long, inputIndex: Int,
                       outputIndex: Int, ergoTreeHex: String, address: Address, assets: Seq[ExtendedTokenData],
                       registers: Registers)

  case class TransactionData(id: ErgoId, blockId: ErgoId, heightIncluded: Int, time: Long, index: Int,
                             confirmations: Int, inputs: Seq[Input], dataInputs: Seq[DataInput],
                             outputs: Seq[Output], size: Int)

  case class AddressBalance(nanoErg: Long, assets: Seq[TokenData])
  case class FullBalance(confirmedBal: AddressBalance, unconfirmedBal: AddressBalance)

  case class BlockTransaction(id: ErgoId, headerId: ErgoId, heightIncluded: Int, time: Long, index: Int,
                              confirmationCount: Int, inputs: Seq[Input], dataInputs: Seq[DataInput],
                              outputs: Seq[Output])
  case class HeaderData(id: ErgoId, parentId: ErgoId, version: Int, height: Int, epoch: Int, difficulty: String,
                        adProofsRoot: String, stateRoot: String, transactionsRoot: String, time: Long, nBits: Long,
                        size: Int, extensnsionHash: String, powSolution: PowSolutionInfo, votes: Seq[Int])
  case class PoWSolutionData(pk: String, w: String, n: String, d: String)
  case class BlockExtension(headerId: ErgoId, digest: String, fields: Fields)
  case class BlockData(headerInfo: HeaderData, blockTxs: Seq[TransactionInfo1], blockExtensionInfo: BlockExtensionInfo, adProofs: String)

  case class BlockContainer(blockData: BlockData, blockReferenceData: BlockReferencesInfo)

  object RegisterData extends ExplorerConversion[AdditionalRegister, RegisterData]{
    def fromExplorer(reg: AdditionalRegister): RegisterData = {
      RegisterData(reg.serializedValue, reg.sigmaType, reg.renderedValue)
    }
  }
  object Registers extends ExplorerConversion[AdditionalRegisters, Registers]{
    def fromExplorer(regs: AdditionalRegisters): Registers = {
      val regMap = regs.asScala
      Registers(  RegisterData.fromOption(regMap.get("R4")), RegisterData.fromOption(regMap.get("R5")),
        RegisterData.fromOption(regMap.get("R6")), RegisterData.fromOption(regMap.get("R7")),
        RegisterData.fromOption(regMap.get("R8")), RegisterData.fromOption(regMap.get("R9"))  )
    }
  }
  object HeaderData extends ExplorerConversion[HeaderInfo, HeaderData]{
    override def fromExplorer(exp: HeaderInfo): HeaderData =
      HeaderData(Helpers.toId(exp.getId), Helpers.toId(exp.getParentId), exp.getVersion, exp.getHeight,
        exp.getEpoch, exp.getDifficulty, exp.getAdProofsRoot, exp.getStateRoot, exp.getTransactionsRoot,
        exp.getTimestamp, exp.getNBits, exp.getSize, exp.getExtensionHash, exp.getPowSolutions, exp.getVotes.asScala.toSeq.map(_.toInt))
  }

  object BlockData extends ExplorerConversion[FullBlockInfo, BlockData]{
    override def fromExplorer(exp: FullBlockInfo): BlockData =
      BlockData(HeaderData.fromExplorer(exp.getHeader), exp.getBlockTransactions.asScala.toSeq,
        exp.getExtension, exp.getAdProofs)
  }
  object BlockContainer extends ExplorerConversion[BlockSummary, BlockContainer]{
    override def fromExplorer(exp: BlockSummary): BlockContainer =
      BlockContainer(BlockData.fromExplorer(exp.getBlock), exp.getReferences)
  }

  object Output extends ExplorerConversion[OutputInfo, Output]{
    def fromExplorer(outputInfo: OutputInfo): Output = {

      Output(
        Helpers.toId(outputInfo.getBoxId), Helpers.toId(outputInfo.getTransactionId), Helpers.toId(outputInfo.getBlockId),
        outputInfo.getValue, outputInfo.getIndex, outputInfo.getCreationHeight, outputInfo.getSettlementHeight,
        outputInfo.getErgoTree, Address.create(outputInfo.getAddress), ExtendedTokenData.fromExplorerList(outputInfo.getAssets),
        Registers.fromExplorer(outputInfo.getAdditionalRegisters), Option(outputInfo.getSpentTransactionId).map(str => ErgoId.create(str)), outputInfo.isMainChain
      )

    }
  }

  object Input extends ExplorerConversion[InputInfo, Input]{
    def fromExplorer(inputInfo: InputInfo): Input = {

      Input(
        Helpers.toId(inputInfo.getBoxId), Helpers.toId(inputInfo.getOutputTransactionId), Helpers.toId(inputInfo.getOutputBlockId),
        inputInfo.getValue, inputInfo.getIndex, inputInfo.getSpendingProof,
        inputInfo.getOutputIndex, inputInfo.getErgoTree, Address.create(inputInfo.getAddress),
        ExtendedTokenData.fromExplorerList(inputInfo.getAssets), Registers.fromExplorer(inputInfo.getAdditionalRegisters)
      )

    }
  }

  object DataInput extends ExplorerConversion[DataInputInfo, DataInput]{
    def fromExplorer(inputInfo: DataInputInfo): DataInput = {

      DataInput(
        Helpers.toId(inputInfo.getBoxId), Helpers.toId(inputInfo.getOutputTransactionId), Helpers.toId(inputInfo.getOutputBlockId),
        inputInfo.getValue, inputInfo.getIndex, inputInfo.getOutputIndex, inputInfo.getErgoTree, Address.create(inputInfo.getAddress),
        ExtendedTokenData.fromExplorerList(inputInfo.getAssets), Registers.fromExplorer(inputInfo.getAdditionalRegisters)
      )

    }
  }
  object TransactionData extends ExplorerConversion[TransactionInfo, TransactionData]{
    def fromExplorer(txInfo: TransactionInfo): TransactionData = {
      TransactionData(
        ErgoId.create(txInfo.getId), ErgoId.create(txInfo.getBlockId), txInfo.getInclusionHeight,
        txInfo.getTimestamp, txInfo.getIndex, txInfo.getNumConfirmations, Input.fromExplorerList(txInfo.getInputs),
        DataInput.fromExplorerList(txInfo.getDataInputs), Output.fromExplorerList(txInfo.getOutputs), txInfo.getSize
      )
    }
  }

  object TokenData extends ExplorerConversion[TokenAmount, TokenData]{
    def fromExplorer(asset: TokenAmount): TokenData = {
      TokenData(Helpers.toId(asset.getTokenId), asset.getAmount.longValue(), asset.getName, asset.getDecimals)
    }
  }
  object ExtendedTokenData extends ExplorerConversion[AssetInstanceInfo, ExtendedTokenData]{
    def fromExplorer(asset: AssetInstanceInfo): ExtendedTokenData = {
      ExtendedTokenData(Helpers.toId(asset.getTokenId), asset.getAmount.longValue(), asset.getIndex, asset.getName, asset.getDecimals, asset.getType)
    }
  }

  object AddressBalance extends ExplorerConversion[Balance, AddressBalance]{
    def fromExplorer(bal: Balance): AddressBalance = {
      AddressBalance(bal.getNanoErgs, TokenData.fromExplorerList(bal.getTokens))
    }
  }
  object FullBalance extends ExplorerConversion[TotalBalance, FullBalance] {
    def fromExplorer(totalBalance: TotalBalance): FullBalance = {
      FullBalance(AddressBalance.fromExplorer(totalBalance.getConfirmed), AddressBalance.fromExplorer(totalBalance.getUnconfirmed))
    }
  }
}