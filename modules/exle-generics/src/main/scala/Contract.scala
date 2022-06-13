import org.ergoplatform.appkit.{Address, BlockchainContext, Constants, ConstantsBuilder, ErgoContract}
import sigmastate.Values

/**
  * Wrapper class representing an ErgoScript contract
  * @param ergoContract ErgoContract to wrap
  * @param constantMap An optional map of constants, to help easily keep track of the constants used to create
  *                    the underlying ErgoContract
  */
case class Contract(ergoContract: ErgoContract, constantMap: Option[Seq[(String, Any)]] = None) {
  def ergoTree:   Values.ErgoTree = ergoContract.getErgoTree
  def address:    Address         = ergoContract.toAddress
  def ergoConstants:  Constants   = ergoContract.getConstants

  def substConstants(name: String, value: String): Contract = {
    Contract(ergoContract.substConstant(name, value))
  }
}

object Contract {
  /**
    * Compiles contract and sets optional constant map for easy access to constant values after creation
    * @param script ErgoScript to compile with
    * @param constants A sequence of constant mappings
    * @param ctx Implicit context used to compile contract
    * @return A compiled Contract, with optional constants map set
    */
  def build(script: String, constants: (String, Any)*)(implicit ctx: BlockchainContext): Contract = {
    val builder = new ConstantsBuilder
    val ergoConstants = {
      constants.foreach(c => builder.item(c._1, c._2))
      builder.build()
    }
    Contract(ctx.compileContract(ergoConstants, script), Some(constants))
  }

  def fromErgoTree(ergoTree: Values.ErgoTree)(implicit ctx: BlockchainContext): Contract = {
    Contract(ctx.newContract(ergoTree))
  }
}
