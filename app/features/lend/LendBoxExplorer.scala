package features.lend

import config.Configs
import lendcore.components.ergo.TxState.TxState
import lendcore.components.errors.{connectionException, explorerException, parseException}
import lendcore.core.SingleLender.Ergs.boxes.{SingleLenderLendBox, SingleLenderRepaymentBox}
import lendcore.core.SingleLender.Ergs.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, RepaymentDetailsRegister, SingleLenderRegister}
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{BlockchainContext, ErgoClientException, ErgoId, ErgoValue, InputBox}
import play.api.Logger
import io.circe.{Json => ciJson}
import lendcore.components.common.StackTrace
import lendcore.components.ergo.{Client, Explorer}
import lendcore.core.SingleLender.Ergs.LendServiceTokens
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

  def getLendBoxes(offset: Int, limit: Int): List[SingleLenderLendBox] = {
    try {
      val boxes = getUnspentTokenBoxes(LendServiceTokens.lendTokenString, offset, limit)
      val lendBoxes = getJsonBoxesViaId(boxes, LendServiceTokens.lendTokenString).map(jsonToWrappedLendBox(_))

      lendBoxes.toList
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

  def getRepaymentBoxes(offset: Int, limit: Int): List[SingleLenderRepaymentBox] = {
    try {
      val boxes = getUnspentTokenBoxes(LendServiceTokens.repaymentTokenString, offset, limit)
      val repaymentBoxes = getJsonBoxesViaId(boxes, LendServiceTokens.repaymentTokenString).map(jsonToWrappedRepaymentBox(_))

      repaymentBoxes.toList
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

  //<editor-fold desc="Json Functions">

  def getJsonBoxesViaId(boxesJson: ciJson, tokenString: String): Seq[ciJson] = {
    if (boxesJson.isNull) {
      throw new NullPointerException("Boxes Json is null")
    }

    boxesJson.hcursor.downField("items").as[Seq[ciJson]].getOrElse(throw new Throwable("parse error"))
      .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).size == 1)
      .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).head
        .hcursor.downField("tokenId").as[String].getOrElse("") == tokenString)
  }

  def jsonToWrappedLendBox(lendBoxJson: ciJson): SingleLenderLendBox = {
    val id: String = lendBoxJson.hcursor.downField("boxId").as[String].getOrElse("")
    val value: Long = lendBoxJson.hcursor.downField("value").as[Long].getOrElse(0)
    val registers = lendBoxJson.hcursor.downField("additionalRegisters").as[ciJson].getOrElse(null)
    val tokens = lendBoxJson.hcursor.downField("assets").as[ciJson].getOrElse(null)

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
    val r7: Option[ciJson] = Option(registers.hcursor.downField("R7").as[ciJson].getOrElse(null))

    var r7Value: Array[Byte] = Array.emptyByteArray
    if (!r7.isEmpty) {
      r7Value = ErgoValue.fromHex(r7.get
        .hcursor.downField("serializedValue").as[String].getOrElse(""))
        .getValue.asInstanceOf[Coll[Byte]].toArray
    }

    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
    val borrowerRegister = new BorrowerRegister(r6)
    val lenderRegister = if (!r7Value.isEmpty) new SingleLenderRegister(r7Value) else SingleLenderRegister.emptyRegister

    val lendBox = new SingleLenderLendBox(
      value,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister,
      lenderRegister,
      id = ErgoId.create(id))

    lendBox
  }

  def jsonToWrappedRepaymentBox(repaymentBoxJson: ciJson): SingleLenderRepaymentBox = {
    val id: String = repaymentBoxJson.hcursor.downField("boxId").as[String].getOrElse("")
    val value: Long = repaymentBoxJson.hcursor.downField("value").as[Long].getOrElse(0)
    val registers = repaymentBoxJson.hcursor.downField("additionalRegisters").as[ciJson].getOrElse(null)
    val tokens = repaymentBoxJson.hcursor.downField("assets").as[ciJson].getOrElse(null)

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
    val r8: Array[Long] = ErgoValue.fromHex(registers.hcursor.downField("R8").as[ciJson].getOrElse(null)
      .hcursor.downField("serializedValue").as[String].getOrElse(""))
      .getValue.asInstanceOf[Coll[Long]].toArray

    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
    val borrowerRegister = new BorrowerRegister(r6)
    val lenderRegister = new SingleLenderRegister(r7)
    val repaymentDetailsRegister = new RepaymentDetailsRegister(r8)

    val wrappedRepaymentBox = new SingleLenderRepaymentBox(
      value,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister,
      lenderRegister,
      repaymentDetailsRegister,
      id = ErgoId.create(id))

    wrappedRepaymentBox
  }
  //</editor-fold>
}
