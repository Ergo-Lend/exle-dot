package features.lend.dao

case class CreateLendReq(id: Long, name: String, description: String, goal: Long,
                         deadlineHeight: Long, repaymentHeight: Long, interestRatePercent: Long, state: Int, borrowerAddress: String,
                         paymentAddress: String, createTxId: Option[String], timeStamp: String,
                         ttl: Long, deleted: Boolean) extends Request

case class FundLendReq(id: Long, ergAmount: Long, lendDeadline: Long, state: Int, paymentAddress: String,
                       lendToken: String, lendTxID: Option[String], lenderAddress: String,
                       timeStamp: String, ttl: Long, deleted: Boolean) extends Request

case class RepaymentReq(id: Long, ergAmount: Long, repaymentDeadline: Long, state: Int, paymentAddress: String,
                        lendToken: String, repaymentTxID: Option[String], userAddress: String,
                        timeStamp: String, ttl: Long, deleted: Boolean) extends Request

trait Request