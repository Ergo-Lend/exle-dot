package features.lend.handlers

import config.Configs
import ergotools.client.Client
import org.ergoplatform.appkit.{Address, BlockchainContext}

import javax.inject.Inject

class LendRequestProcessor @Inject()(client: Client) {
  def processActiveRepayments(ctx: BlockchainContext): Unit = {
    try {
    }
  }

  def processLendInitiationFunds(ctx: BlockchainContext): Unit = {
    // Take active proxy contract boxes
    // Take service box
    // Enter into initiation tx
    // get output box and send info back to user.
  }
}
