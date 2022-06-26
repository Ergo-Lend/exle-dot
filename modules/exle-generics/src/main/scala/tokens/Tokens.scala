package tokens

import commons.configs.{Token, Tokens}
import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

case class SigUSD(override val value: Long) extends Token {
  override val id: ErgoId = new ErgoId(Tokens.sigUSD.getBytes)
}

object SigUSD {
  val id: ErgoId = new ErgoId(Tokens.sigUSD.getBytes)
}

object TokenHelper {
  def applyFunctionToToken(token: ErgoToken, tokenId: ErgoId)(function: Long => Long): ErgoToken = {
    if (token.getId.equals(tokenId)) {
      new ErgoToken(token.getId, function(token.getValue))
    } else {
      token
    }
  }

  def decrement(token: ErgoToken, tokenId: ErgoId): ErgoToken = {
    applyFunctionToToken(token, tokenId)(x => x - 1)
  }

  def increment(token: ErgoToken, tokenId: ErgoId): ErgoToken = {
    applyFunctionToToken(token, tokenId)(x => x + 1)
  }
}