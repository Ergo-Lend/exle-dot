package features.lend

import config.Configs
import ergotools.{LendServiceTokens, TxState}
import ergotools.TxState.TxState
import ergotools.client.Client
import ergotools.explorer.Explorer
import errors.{connectionException, explorerException, parseException}
import features.lend.boxes.{SingleLenderLendBox, SingleLenderRepaymentBox}
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, RepaymentDetailsRegister, SingleLenderRegister}
import helpers.StackTrace
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{BlockchainContext, ErgoClientException, ErgoId, ErgoValue, InputBox}
import play.api.Logger
import io.circe.{Json => ciJson}
import play.api.libs.json.{JsValue, Json}
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.util.Try

class LendBoxExplorer @Inject()(client: Client) extends Explorer {
  private val logger: Logger = Logger(this.getClass)

  def getServiceBox: InputBox = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        val serviceBoxciJson = getUnspentTokenBoxes(LendServiceTokens.nftString, 0, 100)
        val serviceBoxId = serviceBoxciJson.hcursor.downField("items").as[List[ciJson]].getOrElse(throw parseException())
          .head.hcursor.downField("boxId").as[String].getOrElse("")
        var serviceBox = ctx.getBoxesById(serviceBoxId).head
        val serviceAddress = Configs.addressEncoder.fromProposition(serviceBox.getErgoTree).get.toString
        try {
          serviceBox = findMempoolBox(serviceAddress, serviceBox, ctx)
        } catch {
          case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
        }
        serviceBox
      })
    } catch {
      case _: connectionException => throw connectionException()
      case e: ErgoClientException =>
        logger.warn(e.getMessage)
        throw connectionException()

      case e: explorerException =>
        logger.warn(e.getMessage)
        throw connectionException()

      case _: parseException => throw connectionException()
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")

    }
  }

  def getLendBox(lendBoxId: String): InputBox = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        var lendBox = ctx.getBoxesById(lendBoxId).head
        val lendBoxAddress = Configs.addressEncoder.fromProposition(lendBox.getErgoTree).get.toString
        try {
          lendBox = findMempoolBox(lendBoxAddress, lendBox, ctx)
        } catch {
          case e: Throwable =>
            logger.error("Mempool failed")
        }
        lendBox
      })
    } catch {
      case e: ErgoClientException =>
        logger.warn(e.getMessage)
        throw connectionException()
      case _: connectionException => throw connectionException()
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }
  }

  def getRepaymentBox(repaymentBoxId: String): InputBox = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        var lendBox = ctx.getBoxesById(repaymentBoxId).head
        val lendBoxAddress = Configs.addressEncoder.fromProposition(lendBox.getErgoTree).get.toString
        try {
          lendBox = findMempoolBox(lendBoxAddress, lendBox, ctx)
        } catch {
          case e: Throwable =>
            logger.error("Mempool failed")
        }
        lendBox
      })
    } catch {
      case e: ErgoClientException =>
        logger.warn(e.getMessage)
        throw connectionException()
      case _: connectionException => throw connectionException()
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }
  }

  def getLendBoxes(offset: Int, limit: Int): ListBuffer[SingleLenderLendBox] = {
    try {
      var lendBoxCount = 0
      var lendBoxes: ListBuffer[SingleLenderLendBox] = ListBuffer()
      var explorerOffset: Int = 0
      var boxes = getUnspentTokenBoxes(LendServiceTokens.lendTokenString, 0, 100)
      val total = boxes.hcursor.downField("total").as[Int].getOrElse(0)

      while(lendBoxCount < offset + limit && explorerOffset < total) {
        Try {
          // @todo kelim make this into a function
          // Get boxes where tokenId == ErgoLendBox token
          val items = boxes.hcursor.downField("items").as[Seq[ciJson]].getOrElse(throw new Throwable("parse error"))
            .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).size == 1)
            .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).head
              .hcursor.downField("tokenId").as[String].getOrElse("") == LendServiceTokens.lendTokenString)

          for (i <- items.indices) {
            lendBoxCount += 1
            if (lendBoxCount - 1 >= offset && lendBoxCount <= offset + limit) {
              val id: String = items(i).hcursor.downField("boxId").as[String].getOrElse("")
              val value: Long = items(i).hcursor.downField("value").as[Long].getOrElse(0)
              val registers = items(i).hcursor.downField("additionalRegisters").as[ciJson].getOrElse(null)
              val tokens = items(i).hcursor.downField("assets").as[ciJson].getOrElse(null)

              // @todo kelim make this into a function
              // JSON to Boxes class
              val r4: Array[Long] = ErgoValue.fromHex(registers.hcursor.downField("R4").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Long]].toArray
              val r5: Array[Coll[Byte]] = ErgoValue.fromHex(registers.hcursor.downField("R5").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Coll[Byte]]].toArray
              val r6: Array[Byte] = ErgoValue.fromHex(registers.hcursor.downField("R6").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Byte]].toArray
              val r7: Array[Byte] = ErgoValue.fromHex(registers.hcursor.downField("R7").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Byte]].toArray

              val fundingInfoRegister = new FundingInfoRegister(r4)
              val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
              val borrowerRegister = new BorrowerRegister(r6)
              val lenderRegister = new SingleLenderRegister(r7)

              val lendBox = new SingleLenderLendBox(
                value,
                fundingInfoRegister,
                lendingProjectDetailsRegister,
                borrowerRegister,
                lenderRegister,
                id = ErgoId.create(id))

              lendBoxes += lendBox
            }
          }
        }

        explorerOffset += 100
        boxes = getUnspentTokenBoxes(LendServiceTokens.lendTokenString, explorerOffset, 100)
      }

      // @todo kelim do we return total boxes?
      var totalBoxes = lendBoxCount - offset
      if (lendBoxCount > limit + offset) totalBoxes = limit
      else if (totalBoxes < 0) totalBoxes = 0

      lendBoxes
    } catch {
      case e: connectionException => {
        logger.warn(e.getMessage)
        throw e
      }
      case e: Throwable => {
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error occurred during responding the request")
      }
    }
  }

  def getRepaymentBoxes(offset: Int, limit: Int): ListBuffer[SingleLenderRepaymentBox] = {
    try {
      var repaymentBoxCount = 0
      var repaymentBoxes: ListBuffer[SingleLenderRepaymentBox] = ListBuffer()
      var explorerOffset: Int = 0
      var boxes = getUnspentTokenBoxes(LendServiceTokens.repaymentTokenString, 0, 100)
      val total = boxes.hcursor.downField("total").as[Int].getOrElse(0)

      while(repaymentBoxCount < offset + limit && explorerOffset < total) {
        Try {
          // @todo kelim make this into a function
          // Get boxes where tokenId == ErgoLendBox token
          val items = boxes.hcursor.downField("items").as[Seq[ciJson]].getOrElse(throw new Throwable("parse error"))
            .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).size == 1)
            .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).head
              .hcursor.downField("tokenId").as[String].getOrElse("") == LendServiceTokens.repaymentTokenString)

          for (i <- items.indices) {
            repaymentBoxCount += 1
            if (repaymentBoxCount - 1 >= offset && repaymentBoxCount <= offset + limit) {
              val id: String = items(i).hcursor.downField("boxId").as[String].getOrElse("")
              val value: Long = items(i).hcursor.downField("value").as[Long].getOrElse(0)
              val registers = items(i).hcursor.downField("additionalRegisters").as[ciJson].getOrElse(null)
              val tokens = items(i).hcursor.downField("assets").as[ciJson].getOrElse(null)

              // @todo kelim make this into a function
              // JSON to Boxes class
              val r4: Array[Long] = ErgoValue.fromHex(registers.hcursor.downField("R4").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Long]].toArray
              val r5: Array[Coll[Byte]] = ErgoValue.fromHex(registers.hcursor.downField("R5").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Coll[Byte]]].toArray
              val r6: Array[Byte] = ErgoValue.fromHex(registers.hcursor.downField("R6").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Byte]].toArray
              val r7: Array[Byte] = ErgoValue.fromHex(registers.hcursor.downField("R6").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Byte]].toArray
              val r8: Array[Long] = ErgoValue.fromHex(registers.hcursor.downField("R6").as[ciJson].getOrElse(null)
                .hcursor.downField("serializedValue").as[String].getOrElse(""))
                .getValue.asInstanceOf[Coll[Long]].toArray

              val fundingInfoRegister = new FundingInfoRegister(r4)
              val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
              val borrowerRegister = new BorrowerRegister(r6)
              val lenderRegister = new SingleLenderRegister(r7)
              val repaymentDetailsRegister = new RepaymentDetailsRegister(r8)

              val repaymentBox = new SingleLenderRepaymentBox(
                value,
                fundingInfoRegister,
                lendingProjectDetailsRegister,
                borrowerRegister,
                lenderRegister,
                repaymentDetailsRegister,
                id = ErgoId.create(id))

              repaymentBoxes += repaymentBox
            }
          }
        }

        explorerOffset += 100
        boxes = getUnspentTokenBoxes(LendServiceTokens.lendTokenString, explorerOffset, 100)
      }

      // @todo kelim do we return total boxes?
      var totalBoxes = repaymentBoxCount - offset
      if (repaymentBoxCount > limit + offset) totalBoxes = limit
      else if (totalBoxes < 0) totalBoxes = 0

      repaymentBoxes
    } catch {
      case e: connectionException => {
        logger.warn(e.getMessage)
        throw e
      }
      case e: Throwable => {
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error occurred during responding the request")
      }
    }
  }

  def isBoxInMemPool(box: InputBox) : Boolean = {
    try {
      val address = getAddress(box.getErgoTree.bytes)
      val transactions = Json.parse(getTxsInMempoolByAddress(address.toString).toString())
      if (transactions != null) {
        (transactions \ "items").as[List[JsValue]].exists(tx => {
          if ((tx \ "inputs").as[JsValue].toString().contains(box.getId.toString)) true
          else false
        })
      } else {
        false
      }
    } catch {
      case e: explorerException =>
        logger.warn(e.getMessage)
        throw connectionException()

      case e: parseException =>
        logger.warn(e.getMessage)
        throw connectionException()

      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }
  }

  def findMempoolBox(address: String, box: InputBox, ctx: BlockchainContext): InputBox = {
    try {
      val mempool = Json.parse(getUnconfirmedTxByAddress(address).toString())
      var outBox = box
      val txs = (mempool \ "items").as[List[JsValue]]
      var txMap: Map[String, JsValue] = Map()
      txs.foreach(txJson => {
        val txInput = (txJson \ "inputs").as[List[JsValue]].head
        val id = (txInput \ "id").as[String]
        txMap += (id -> txJson)
      })
      val keys = txMap.keys.toSeq
      logger.debug(outBox.getId.toString)
      logger.debug(keys.toString())
      while (keys.contains(outBox.getId.toString)) {
        val txJson = txMap(outBox.getId.toString)
        val inputs = (txJson \ "inputs").as[JsValue].toString().replaceAll("id", "boxId")
        val outputs = (txJson \ "outputs").as[JsValue].toString().replaceAll("id", "boxId")
          .replaceAll("txId", "transactionId")
        val dataInputs = (txJson \ "dataInputs").as[JsValue].toString()
        val id = (txJson \ "id").as[String]
        val newJson =
          s"""{
              "id": "$id",
              "inputs": $inputs,
              "dataInputs": $dataInputs,
              "outputs": $outputs
             }"""
        val tmpTx = ctx.signedTxFromJson(newJson.replaceAll("null", "\"\""))
        outBox = tmpTx.getOutputsToSpend.get(0)
      }
      outBox
    } catch {
      case e: explorerException =>
        logger.warn(e.getMessage)
        throw connectionException()
      case _: parseException => throw connectionException()
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }
  }

  def checkTransaction(txId: String): TxState = {
    try {
      if (txId != "") {
        val unconfirmedTx = getUnconfirmedTx(txId)
        if (unconfirmedTx == ciJson.Null) {
          val confirmedTx = getConfirmedTx(txId)
          if (confirmedTx == ciJson.Null) {
            // Tx unsuccessful
            TxState.Unsuccessful // resend transaction
          } else {
            TxState.Mined // transaction mined
          }
        } else {
          // in mempool but not mined
          TxState.Mempooled // transaction already in mempool
        }
      } else {
        TxState.Unsuccessful
      }
    } catch {
      case e: explorerException => {
        logger.warn(e.getMessage)
        throw connectionException()
      }
      case e: Throwable => {
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
      }
    }
  }

  def getAddress(addressBytes: Array[Byte]): ErgoAddress = {
    val ergoTree = ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
    Configs.addressEncoder.fromProposition(ergoTree).get
  }
}
