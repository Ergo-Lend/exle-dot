package commons.node

import commons.configs.NodeConfig
import commons.configs.NodeConfig.SystemNodeConfig
import commons.ergo.ErgCommons
import commons.errors.ConnectionException
import org.ergoplatform.appkit.BoxOperations.ExplorerApiUnspentLoader
import org.ergoplatform.appkit.{
  Address,
  BoxOperations,
  CoveringBoxes,
  ErgoClient,
  ErgoToken,
  InputBox,
  NetworkType,
  RestApiErgoClient
}
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
      client = RestApiErgoClient.create(
        SystemNodeConfig.nodeUrl,
        NodeConfig.networkType,
        "",
        SystemNodeConfig.explorerUrl
      )
      client.execute { ctx =>
        System.out.println(
          s"Client Instantiated, Current Height: ${ctx.getHeight} " +
            s"Network: ${NodeConfig.networkType}"
        )
        ctx.getHeight
      }
    } catch {
      case e: Throwable =>
        logger.error(message = s"Could not set client! ${e.getMessage}.")
    }
  }

  def getClient: ErgoClient =
    client

  /**
    * @return current height of the blockchain
    */
  def getHeight: Long =
    try {
      client.execute(ctx => ctx.getHeight.toLong)
    } catch {
      case _: Throwable => throw ConnectionException()
    }

  def getAllUnspentBox(address: Address): List[InputBox] =
    client.execute(ctx =>
      try {
        val unspent = ctx.getDataSource.getUnspentBoxesFor(address, 0, 100)

        unspent.asScala.toList
      } catch {
        case e: Throwable =>
          throw ConnectionException(e.getMessage)
      }
    )

  def getCoveringBoxesFor(address: Address, amount: Long): CoveringBoxes =
    client.execute(ctx =>
      try {
        val amountMinusMinerFee: Long = amount - ErgCommons.MinMinerFee
        val boxOperations = BoxOperations.createForSender(address, ctx)
        val inputBoxList =
          boxOperations.withAmountToSpend(amountMinusMinerFee).loadTop()

        val coveringBoxes =
          new CoveringBoxes(amount, inputBoxList, null, false)

        coveringBoxes
      } catch {
        case _: Throwable => throw ConnectionException()
      }
    )

  def getCoveringBoxesFor(
    address: Address,
    amount: Long,
    tokensToSpend: java.util.List[ErgoToken]
  ): List[InputBox] =
    client.execute(ctx =>
      try {
        val amountMinusMinerFee: Long = amount - ErgCommons.MinMinerFee
        val boxOperations = BoxOperations.createForSender(address, ctx)
        val coveringBoxes = boxOperations
          .withAmountToSpend(amountMinusMinerFee)
          .withTokensToSpend(tokensToSpend)
          .loadTop()

        coveringBoxes.asScala.toList
      } catch {
        case _: Throwable => throw ConnectionException()
      }
    )
}
