package pay

import commons.ergo.{ContractUtils, ErgCommons}
import commons.configs.TestNetNodeConfig
import commons.configs.MainNetNodeConfig
import org.ergoplatform.appkit.{
  Address,
  BoxOperations,
  ErgoContract,
  ErgoToken,
  NetworkType,
  ReducedTransaction,
  RestApiErgoClient,
  UnsignedTransaction
}

object ErgoPayUtils {

  /**
    * ReducedTx for sending Ergs
    *
    * @param amountToSend Amount of Ergs to send
    * @param sender       Sender of tx
    * @param recipient    Recipient of Tx
    * @param isMainNet    are we using mainnet chain
    * @return ReducedTx for sender to sign
    */
  def getReducedSendTx(
    amountToSend: Long,
    sender: Address,
    recipient: Address,
    isMainNet: Boolean
  ): ReducedTransaction = {
    val networkType: NetworkType =
      if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET;
    RestApiErgoClient
      .create(
        getDefaultNodeUrl(isMainNet),
        networkType,
        "",
        RestApiErgoClient.getDefaultExplorerUrl(networkType)
      )
      .execute { ctx =>
        val recipientContract: ErgoContract = ContractUtils.sendToPK(recipient)
        val unsignedTransaction: UnsignedTransaction =
          BoxOperations
            .createForSender(sender, ctx)
            .withAmountToSpend(amountToSend)
            .putToContractTxUnsigned(recipientContract)

        ctx.newProverBuilder().build().reduce(unsignedTransaction, 0)
      }
  }

  /**
    * @param amountToSend  Amount of Ergs to send
    * @param tokensToSpend tokens that sender are gonna send to recipient
    * @param sender        Sender of tx
    * @param recipient     Recipient of Tx
    * @param isMainNet     are we using mainnet chain
    * @return ReducedTx for sender to sign
    */
  def getReducedSendTx(
    amountToSend: Long,
    tokensToSpend: java.util.List[ErgoToken],
    sender: Address,
    recipient: Address,
    isMainNet: Boolean
  ): ReducedTransaction = {
    val networkType: NetworkType =
      if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET;
    RestApiErgoClient
      .create(
        getDefaultNodeUrl(isMainNet),
        networkType,
        "",
        RestApiErgoClient.getDefaultExplorerUrl(networkType)
      )
      .execute { ctx =>
        val recipientContract: ErgoContract = ContractUtils.sendToPK(recipient)
        val unsignedTransaction: UnsignedTransaction =
          BoxOperations
            .createForSender(sender, ctx)
            .withAmountToSpend(amountToSend)
            .withTokensToSpend(tokensToSpend)
            .putToContractTxUnsigned(recipientContract)

        ctx.newProverBuilder().build().reduce(unsignedTransaction, 0)
      }
  }

  /**
    * Create a reducedTx that sends only tokens to recipient
    *
    * @param tokensToSpend tokens that sender are gonna send to recipient
    * @param sender        Sender of tx
    * @param recipient     Recipient of Tx
    * @param isMainNet     are we using mainnet chain
    * @return ReducedTx for sender to sign
    */
  def getReducedSendTokensTx(
    tokensToSpend: java.util.List[ErgoToken],
    sender: Address,
    recipient: Address,
    isMainNet: Boolean
  ): ReducedTransaction = {
    val networkType: NetworkType =
      if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET;
    RestApiErgoClient
      .create(
        getDefaultNodeUrl(isMainNet),
        networkType,
        "",
        RestApiErgoClient.getDefaultExplorerUrl(networkType)
      )
      .execute { ctx =>
        val recipientContract: ErgoContract = ContractUtils.sendToPK(recipient)
        val unsignedTransaction: UnsignedTransaction =
          BoxOperations
            .createForSender(sender, ctx)
            // @todo kii, check if we need this min box
            .withAmountToSpend(ErgCommons.MinBoxFee)
            .withTokensToSpend(tokensToSpend)
            .putToContractTxUnsigned(recipientContract)

        ctx.newProverBuilder().build().reduce(unsignedTransaction, 0)
      }
  }

  def isMainNetAddress(address: String): Boolean =
    try {
      Address.create(address).isMainnet
    } catch {
      case e: Throwable =>
        throw new IllegalArgumentException("Invalid address: " + address);
    }

  def getDefaultNodeUrl(isMainNet: Boolean): String =
    if (isMainNet) MainNetNodeConfig.nodeUrl else TestNetNodeConfig.nodeUrl;
}
