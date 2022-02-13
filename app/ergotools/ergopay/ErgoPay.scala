package ergotools.ergopay

import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, BoxOperations, ErgoClient, ErgoToken, InputBox, NetworkType, Parameters, ReducedTransaction, RestApiErgoClient, UnsignedTransaction, UnsignedTransactionBuilder}

import java.util.{Base64, Collections}

object ErgoPay {
  val NODE_MAINNET: String = "http://213.239.193.208:9053/"
  val NODE_TESTNET: String = "http://213.239.193.208:9052/"

  def roundTrip(address: String): ErgoPayResponse = {
    try {
      val isMainNet: Boolean = isMainNetAddress(address)
      val amountToSend: Long = Parameters.MinFee * 10

      val sender: Address = Address.create(address)
      val recipient: Address = Address.create(address)

      val reduced: Array[Byte] = getReducedSendTx(isMainNet, amountToSend, sender, recipient).toBytes

      val response: ErgoPayResponse = new ErgoPayResponse(
        reducedTx = Base64.getUrlEncoder().encodeToString(reduced),
        address = address,
        message = "Here is your 1 ERG round trip.",
        messageSeverity = Severity.INFORMATION
      )

      response
    } catch {
      case e: Throwable => {
        val response: ErgoPayResponse = new ErgoPayResponse(
          reducedTx = "",
          address = address,
          message = e.getMessage,
          messageSeverity = Severity.ERROR
        )
        response
      }
    }
  }

  // <editor-fold description="Functions">

  def getReducedSendTx(isMainNet: Boolean,
                       amountToSend: Long,
                       sender: Address,
                       recipient: Address): ReducedTransaction = {
    return getErgoClient(isMainNet).execute(ctx => {
      val contract: ErgoTreeContract = new ErgoTreeContract(recipient.getErgoAddress().script)
      val unsignedTx: UnsignedTransaction = BoxOperations.putToContractTxUnsigned(
        ctx,
        Collections.singletonList(sender),
        contract,
        amountToSend,
        Collections.emptyList())
      ctx.newProverBuilder().build().reduce(unsignedTx, 0)
    })
  }

  def getReducedTx(isMainNet: Boolean,
                   amountToSpend: Long,
                   tokensToSpend: java.util.List[ErgoToken],
                   sender: Address,
                   outputBuilder: (UnsignedTransactionBuilder => UnsignedTransactionBuilder)): ReducedTransaction = {
    getErgoClient(isMainNet).execute(ctx => {
      val boxesToSpend: java.util.List[InputBox] = BoxOperations.loadTop(
        ctx,
        Collections.singletonList(sender),
        amountToSpend + Parameters.MinFee,
        tokensToSpend
      )

      val changeAddress: P2PKAddress = sender.asP2PK()
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      val unsignedTransactionBuilder: UnsignedTransactionBuilder =
        txB.boxesToSpend(boxesToSpend)
          .fee(Parameters.MinFee)
          .sendChangeTo(changeAddress)

      val unsignedTransaction: UnsignedTransaction = outputBuilder.apply(unsignedTransactionBuilder).build();

      ctx.newProverBuilder().build.reduce(unsignedTransaction, 0)
    })
  }

  def isMainNetAddress(address: String): Boolean= {
    try {
      Address.create(address).isMainnet()
    } catch {
      case _: Throwable => throw new IllegalArgumentException(s"Invalid address: ${address}")
    }
  }

  def getDefaultNodeUrl(isMainNet: Boolean) = {
    if (isMainNet)
      NODE_MAINNET
    else
      NODE_TESTNET
  }

  def getErgoClient(isMainNet: Boolean): ErgoClient = {
    val networkType: NetworkType = if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET
    RestApiErgoClient.create(
      getDefaultNodeUrl(isMainNet),
      networkType,
      "",
      RestApiErgoClient.getDefaultExplorerUrl(networkType)
    )
  }

  // </editor-fold>
}
