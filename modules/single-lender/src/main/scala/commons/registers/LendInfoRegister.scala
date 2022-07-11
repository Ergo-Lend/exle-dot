package commons.registers

import commons.boxes.registers.RegisterTypes.{
  CollByte,
  LongRegister,
  RegisterHelpers
}
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
  * @param creationHeight
  */
final case class FundingInfoRegister(
  fundingGoal: Long,
  deadlineHeight: Long,
  interestRatePercent: Long,
  repaymentHeightLength: Long,
  creationHeight: Long
) extends LongRegister {

  def this(registerData: Array[Long]) =
    this(
      fundingGoal = registerData(0),
      deadlineHeight = registerData(1),
      interestRatePercent = registerData(2),
      repaymentHeightLength = registerData(3),
      creationHeight = registerData(4)
    )

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](5)

    register(0) = fundingGoal
    register(1) = deadlineHeight
    register(2) = interestRatePercent
    register(3) = repaymentHeightLength
    register(4) = creationHeight

    ergoValueOf(register)
  }

  /**
    * Interest Rate are in 10ths as it can go to Decimals.
    * For example, 10.8% would be 108. Therefore we divvy by 10
    * here.
    * @return
    */
  def interestRateAsDouble: Double = {
    val interestRate = interestRatePercent.toDouble / 10

    interestRate
  }
}

final case class LendingProjectDetailsRegister(
  projectName: String,
  description: String
) extends RegisterHelpers {

  def this(registerData: Array[Coll[Byte]]) = this(
    projectName = CollByte.collByteToString(registerData(0)),
    description = CollByte.collByteToString(registerData(1))
  )

  def toRegister: ErgoValue[Coll[Coll[Byte]]] = {
    val register: Array[Array[Byte]] = new Array[Array[Byte]](2)

    register(0) = stringToCollByte(projectName)
    register(1) = stringToCollByte(description)

    ergoValueOf(register)
  }
}

final case class CreationInfoRegister(creationHeight: Long, version: Long = 1)
    extends LongRegister {

  def this(registerData: Array[Long]) =
    this(creationHeight = registerData(0), version = registerData(1))

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](2)

    register(0) = creationHeight
    register(1) = version

    ergoValueOf(register)
  }
}

final case class ServiceBoxInfoRegister(name: String, description: String)
    extends RegisterHelpers {

  def this(registerData: Array[Coll[Byte]]) = this(
    name = CollByte.collByteToString(registerData(0)),
    description = CollByte.collByteToString(registerData(1))
  )

  def toRegister: ErgoValue[Coll[Coll[Byte]]] = {
    val register: Array[Array[Byte]] = new Array[Array[Byte]](2)

    register(0) = stringToCollByte(name)
    register(1) = stringToCollByte(description)

    ergoValueOf(register)
  }
}

// For Percentage, we're using /1000 rather than /100, so that we can get 1 decimal
final case class ProfitSharingRegister(
  profitSharingPercentage: Long,
  serviceFeeAmount: Long
) extends LongRegister {
  def this(registerData: Array[Long]) =
    this(
      profitSharingPercentage = registerData(0),
      serviceFeeAmount = registerData(1)
    )

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](2)

    register(0) = profitSharingPercentage
    register(1) = serviceFeeAmount

    ergoValueOf(register)
  }
}
