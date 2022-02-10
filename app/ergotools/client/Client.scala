package ergotools.client

import config.Configs
import org.ergoplatform.appkit.{Address, CoveringBoxes, ErgoClient, ErgoToken, InputBox, RestApiErgoClient}
import play.api.Logger
import errors.connectionException

import scala.collection.JavaConverters._
import javax.inject.Singleton

@Singleton
class Client {
  private val logger: Logger = Logger(this.getClass)
  private var client: ErgoClient = _

  def setClient(): Long = {
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
        0L
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
        ctx.getCoveringBoxesFor(address, (1e9 * 1e8).toLong, null).getBoxes.asScala.toList
      } catch {
        case _: Throwable => throw connectionException()
      }
    )
  }

  def getCoveringBoxesFor(address: Address, amount: Long): CoveringBoxes = {
    client.execute(ctx =>
      try {
        ctx.getCoveringBoxesFor(address, amount, null)
      } catch {
        case _: Throwable => throw connectionException()
      }
    )
  }
}
