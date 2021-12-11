package features.lend

import config.Configs
import ergotools.TxState
import ergotools.TxState.TxState
import ergotools.client.Client
import ergotools.explorer.Explorer
import errors.{connectionException, explorerException, parseException}
import helpers.StackTrace
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{BlockchainContext, ErgoClientException, InputBox}
import play.api.Logger
import io.circe.{Json => ciJson}
import play.api.libs.json.{JsValue, Json}
import sigmastate.serialization.ErgoTreeSerializer

import javax.inject.Inject
import scala.util.Try

class LendBoxExplorer @Inject()(client: Client) extends Explorer {
  private val logger: Logger = Logger(this.getClass)

  def getServiceBox: InputBox = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        val serviceBoxciJson = getUnspentTokenBoxes(Configs.token.nft, 0, 100)
        val serviceBoxId = serviceBoxciJson.hcursor.downField("items").as[List[ciJson]].getOrElse(throw parseException())
          .head.hcursor.downField("boxId").as[String].getOrElse("")
        var serviceBox = ctx.getBoxesById(serviceBoxId).head
        val serviceAddress = Configs.addressEncoder.fromProposition(serviceBox.getErgoTree).get.toString
        try {
          serviceBox = findMempoolBox(serviceAddress, serviceBox, ctx)
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

  def getLendBox(tokenId: String): InputBox = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        var lendBoxId: String = ""
        var C: Int = 0
        while (lendBoxId == "") {
          Try {
            val lendBoxciJson = getUnspentTokenBoxes(Configs.token.service, C, 100)
            lendBoxId = lendBoxciJson.hcursor.downField("items").as[List[ciJson]].getOrElse(null)
              .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null).size > 1)
              .filter(_.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null)(1)
                .hcursor.downField("tokenId").as[String].getOrElse("") == tokenId).head
              .hcursor.downField("boxId").as[String].getOrElse("")
          }
          C += 100
        }

        var lendBox = ctx.getBoxesById(lendBoxId).head
        val lendBoxAddress = Configs.addressEncoder.fromProposition(lendBox.getErgoTree).get.toString
        try {
          lendBox = findMempoolBox(lendBoxAddress, lendBox, ctx)
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
