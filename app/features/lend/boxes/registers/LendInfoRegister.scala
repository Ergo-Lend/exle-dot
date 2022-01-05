package features.lend.boxes.registers

import boxes.registers.RegisterTypes.{CollByte, CollByteRegister, LongRegister}
import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll

// We want an input and output.
// Created with data,
// Input into box the data.

/**
 * @todo serviceFee
 *
 * @param fundingGoal
 * @param deadlineHeight
 * @param interestRatePercent
 * @param repaymentHeightLength
 */
case class FundingInfoRegister(fundingGoal: Long,
                          deadlineHeight: Long,
                          interestRatePercent: Long,
                          repaymentHeightLength: Long) extends LongRegister {

  def this(registerData: Array[Long]) = this(
    fundingGoal = registerData(0),
    deadlineHeight = registerData(1),
    interestRatePercent = registerData(2),
    repaymentHeightLength = registerData(3))

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](4)

    register(0) = fundingGoal
    register(1) = deadlineHeight
    register(2) = interestRatePercent
    register(3) = repaymentHeightLength

    ergoValueOf(register)
  }
}

case class LendingProjectDetailsRegister(projectName: String,
                                    description: String,
                                    borrowersPubKey: String)  extends CollByteRegister {

  def this(registerData: Array[Coll[Byte]]) = this(
    projectName = CollByte.collByteToString(registerData(0)),
    description = CollByte.collByteToString(registerData(1)),
    borrowersPubKey = CollByte.collByteToString(registerData(2))
  )

  def toRegister: ErgoValue[Coll[Coll[Byte]]] = {
    val register: Array[Array[Byte]] = new Array[Array[Byte]](3)

    register(0) = stringToCollByte(projectName)
    register(1) = stringToCollByte(description)
    register(2) = stringToCollByte(borrowersPubKey)

    ergoValueOf(register)
  }
}
