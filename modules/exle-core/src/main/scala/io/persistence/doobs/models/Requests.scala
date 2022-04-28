package io.persistence.doobs.models

case class CreateLendReq(override val id: Long,
                         name: String,
                         description: String,
                         goal: Long,
                         creationHeight: Long,
                         deadlineHeight: Long,
                         repaymentHeight: Long,
                         interestRate: Long,
                         override val state: Int,
                         borrowerAddress: String,
                         override val paymentAddress: String,
                         createTxId: Option[String],
                         timeStamp: String,
                         ttl: Long,
                         deleted: Boolean)
  extends ProxyReq(id, paymentAddress, state, createTxId)

case class FundLendReq(override val id: Long,
                       lendBoxId: String,
                       ergAmount: Long,
                       override val state: Int,
                       override val paymentAddress: String,
                       lendTxID: Option[String],
                       lenderAddress: String,
                       timeStamp: String,
                       ttl: Long,
                       deleted: Boolean)
  extends ProxyReq(id, paymentAddress, state, lendTxID)

case class FundRepaymentReq(override val id: Long,
                        repaymentBoxId: String,
                        ergAmount: Long,
                        override val state: Int,
                        override val paymentAddress: String,
                        repaymentTxID: Option[String],
                        userAddress: String,
                        timeStamp: String,
                        ttl: Long,
                        deleted: Boolean)
  extends ProxyReq(id, paymentAddress, state, repaymentTxID)

class ProxyReq(val id: Long, val paymentAddress: String, val state: Int, val txId: Option[String])