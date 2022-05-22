package chain.explorer

import chain.explorer.Models.{
  AddressBalance,
  BlockContainer,
  FullBalance,
  Output,
  TransactionData
}
import org.ergoplatform.appkit.{Address, ErgoId, NetworkType, RestApiErgoClient}
import org.ergoplatform.explorer.client.model._
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import retrofit2.Response
import sigmastate.Values.ErgoTree

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class ExplorerHandler(networkType: NetworkType) {
  private val url: String = RestApiErgoClient.getDefaultExplorerUrl(networkType)
  private val apiClient = new ExplorerApiClient(url)

  private val apiService: DefaultApi =
    apiClient.createService(classOf[DefaultApi])

  private def asOption[T](resp: Response[T]): Option[T] =
    if (resp.isSuccessful)
      Some(resp.body())
    else
      None

  private def itemSeq[T](opt: Option[Items[T]]) =
    if (opt.isDefined)
      Some(opt.get.getItems.asScala.toSeq)
    else
      None

  private def outputSeq(opt: Option[ItemsA]) =
    if (opt.isDefined)
      Some(opt.get.getItems.asScala.toSeq)
    else
      None

  /**
    * Get data about a transaction using it's id
    * @param id ErgoId of transaction
    */
  def getTransaction(id: ErgoId): Option[TransactionData] =
    TransactionData.fromOption(
      asOption[TransactionInfo](
        apiService.getApiV1TransactionsP1(id.toString).execute()
      )
    )

  /**
    * Get transactions that were sent and received by the given address
    * @param addr Address to check
    * @param offset Number of transactions to offset
    * @param limit Max number of transactions in response
    */
  def getTxsForAddress(
    addr: Address,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[TransactionData]] = {
    val opt = asOption[Items[TransactionInfo]](
      apiService
        .getApiV1AddressesP1Transactions(addr.toString, offset, limit)
        .execute()
    )
    TransactionData.fromOptionSeq(itemSeq[TransactionInfo](opt))
  }

  /**
    * Get commons.boxes under addresses that match a given ErgoTree template hash
    * @param hash Hash of template
    * @param offset Number of commons.boxes to offset
    * @param limit Max number of commons.boxes in response
    */
  def getBoxesByTemplateHash(
    hash: String,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[Output]] =
    Output.fromOptionSeq(
      outputSeq(
        asOption[ItemsA](
          apiService
            .getApiV1BoxesByergotreetemplatehashP1(hash, offset, limit)
            .execute()
        )
      )
    )

  /**
    * Get commons.boxes under addresses that match a given ErgoTree
    * @param ergoTree ErgoTree to find commons.boxes for
    * @param offset Number of commons.boxes to offset
    * @param limit Max number of commons.boxes in response
    */
  def getBoxesByErgoTree(
    ergoTree: ErgoTree,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[Output]] =
    Output.fromOptionSeq(
      outputSeq(
        asOption[ItemsA](
          apiService
            .getApiV1BoxesByergotreeP1(ergoTree.bytesHex, offset, limit)
            .execute()
        )
      )
    )

  /**
    * Get commons.boxes under addresses that match a given ErgoTree hexadecimal string
    * @param ergoTreeHex ErgoTree hex string to find commons.boxes for
    * @param offset Number of commons.boxes to offset
    * @param limit Max number of commons.boxes in response
    */
  def getBoxesByErgoTreeHex(
    ergoTreeHex: String,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[Output]] =
    Output.fromOptionSeq(
      outputSeq(
        asOption[ItemsA](
          apiService
            .getApiV1BoxesByergotreeP1(ergoTreeHex, offset, limit)
            .execute()
        )
      )
    )

  /**
    * Get commons.boxes under a certain address
    * @param address Address to find commons.boxes for
    * @param offset Number of commons.boxes to offset
    * @param limit Max number of commons.boxes in response
    */
  def getBoxesByAddress(
    address: Address,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[Output]] =
    Output.fromOptionSeq(
      outputSeq(
        asOption[ItemsA](
          apiService
            .getApiV1BoxesByaddressP1(address.toString, offset, limit)
            .execute()
        )
      )
    )

  /**
    * Get commons.boxes that contain a token with the given id
    * @param tokenId ErgoId of token that the requested commons.boxes must contain
    * @param offset Number of commons.boxes to offset
    * @param limit Max number of commons.boxes in response
    */
  def getBoxesByTokenId(
    tokenId: ErgoId,
    offset: Int = 0,
    limit: Int = 10
  ): Option[Seq[Output]] =
    Output.fromOptionSeq(
      outputSeq(
        asOption[ItemsA](
          apiService
            .getApiV1BoxesUnspentBytokenidP1(tokenId.toString, offset, limit)
            .execute()
        )
      )
    )

  /**
    * Get the total balance of confirmed and unconfirmed ERG and Tokens under the given address
    * @param address Address to find total balance for
    */
  def getTotalBalance(address: Address): Option[FullBalance] =
    FullBalance.fromOption(
      asOption[TotalBalance](
        apiService.getApiV1AddressesP1BalanceTotal(address.toString).execute()
      )
    )

  /**
    * Get balance for address that only accounts for commons.boxes with a minimum confirmation number
    * @param address Address to get balance for
    * @param minConfirmations Minimum number of confirmations for a box under the address to be included in the balance
    */
  def getConfirmedBalance(
    address: Address,
    minConfirmations: Int = 20
  ): Option[AddressBalance] =
    AddressBalance.fromOption(
      asOption[Balance](
        apiService
          .getApiV1AddressesP1BalanceConfirmed(
            address.toString,
            minConfirmations
          )
          .execute()
      )
    )

  def getBlockById(blockId: ErgoId): Option[BlockContainer] =
    BlockContainer.fromOption(
      asOption[BlockSummary](
        apiService.getApiV1BlocksP1(blockId.toString).execute()
      )
    )
}
