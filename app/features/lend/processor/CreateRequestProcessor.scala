package features.lend.processor

import ergotools.client.Client
import features.lend.LendBoxExplorer
import features.lend.dao.CreateLendReqDAO
import play.api.Logger

import javax.inject.Inject

class CreateRequestProcessor @Inject()(client: Client,
                                       lendBoxExplorer: LendBoxExplorer,
                                       createLendReqDAO: CreateLendReqDAO){
  private val logger: Logger = Logger(this.getClass)

}
