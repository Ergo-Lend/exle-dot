package lendcore.components.ergo

import config.Configs
import lendcore.components.errors.connectionException
import org.ergoplatform.appkit._
import play.api.Logger

import javax.inject.Singleton
import scala.collection.JavaConverters._

@Singleton
class Client() {
  private val logger: Logger = Logger(this.getClass)
  private var client: ErgoClient = _

  def setClient(): Unit = {
    println("Ergo Client Starting up...")
    try {
      client = RestApiErgoClient.create(Configs.nodeUrl, Configs.networkType, "", Configs.explorerUrl)
      client.execute(ctx => {
        System.out.println(s"Client Instantiated, Current Height: ${ctx.getHeight}")
        ctx.getHeight
      })
    } catch {
      case e: Throwable =>
        logger.error(message = s"Could not set client! ${e.getMessage}.")
    }
  }

  def getClient: ErgoClient = {
    client
  }

  /**
   * @return current height of the blockchain
   */
  def getHeight: Long = {
    try {
      client.execute(ctx => ctx.getHeight)
    } catch {
      case _: Throwable => throw connectionException()
    }
  }

  def getUnspentBox(address: Address): List[InputBox] = {
    client.execute(ctx =>
      try {
        ctx.getUnspentBoxesFor(address, 0, 100).asScala.toList
      } catch {
        case _: Throwable => throw connectionException()
      }
    )
  }

  def getAllUnspentBox(address: Address): List[InputBox] = {
    client.execute(ctx =>
      try {
        val nullToken: java.util.List[ErgoToken] = List.empty[ErgoToken].asJava
        ctx.getCoveringBoxesFor(address, (1e9 * 1e8).toLong, nullToken).getBoxes.asScala.toList
      } catch {
        case e: Throwable =>
          throw connectionException(e.getMessage)
      }
    )
  }

  def getCoveringBoxesFor(address: Address, amount: Long): CoveringBoxes = {
    client.execute(ctx =>
      try {
        val nullToken: java.util.List[ErgoToken] = List.empty[ErgoToken].asJava
        ctx.getCoveringBoxesFor(address, amount, nullToken)
      } catch {
        case _: Throwable => throw connectionException()
      }
    )
  }

  def getCoveringBoxesFor(address: Address, amount: Long, tokensToSpend: java.util.List[ErgoToken]): CoveringBoxes = {
    client.execute(ctx =>
      try {
        ctx.getCoveringBoxesFor(address, amount, tokensToSpend)
      } catch {
        case _: Throwable => throw connectionException()
      }
    )
  }
}
