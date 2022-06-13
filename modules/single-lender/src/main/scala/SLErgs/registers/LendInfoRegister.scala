package SLErgs.registers

import commons.boxes.registers.RegisterTypes.{CollByte, CollByteRegister, LongRegister}
import org.ergoplatform.appkit.ErgoValue
import special.collection.{Coll, CollType}

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
case class FundingInfoRegister(
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

  def toRegister: ErgoValue[Coll[java.lang.Long]] = {
    val register: Array[java.lang.Long] = new Array[java.lang.Long](5)

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

case class LendingProjectDetailsRegister(
  projectName: String,
  description: String
) extends CollByteRegister {

  def this(registerData: Array[Coll[Byte]]) = this(
    projectName = CollByte.collByteToString(registerData(0)),
    description = CollByte.collByteToString(registerData(1))
  )

  def toRegister: ErgoValue[Coll[Coll[java.lang.Byte]]] = {
    val register: Array[Array[Byte]] = new Array[Array[Byte]](2)

    register(0) = stringToCollByte(projectName)
    register(1) = stringToCollByte(description)

    ergoValueOf(register)
  }
}

case class CreationInfoRegister(creationHeight: Long, version: Long = 1)
    extends LongRegister {

  def this(registerData: Array[Long]) =
    this(creationHeight = registerData(0), version = registerData(1))

  def toRegister: ErgoValue[Coll[java.lang.Long]] = {
    val register: Array[java.lang.Long] = new Array[java.lang.Long](2)

    register(0) = creationHeight
    register(1) = version

    ergoValueOf(register)
  }
}

case class ServiceBoxInfoRegister(name: String, description: String)
    extends CollByteRegister {

  def this(registerData: Array[Coll[Byte]]) = this(
    name = CollByte.collByteToString(registerData(0)),
    description = CollByte.collByteToString(registerData(1))
  )

  def toRegister: ErgoValue[Coll[Coll[java.lang.Byte]]] = {
    val register: Array[Array[Byte]] = new Array[Array[Byte]](2)

    register(0) = stringToCollByte(name)
    register(1) = stringToCollByte(description)

    ergoValueOf(register)
  }
}

// For Percentage, we're using /1000 rather than /100, so that we can get 1 decimal
case class ProfitSharingRegister(
  profitSharingPercentage: Long,
  serviceFeeAmount: Long
) extends LongRegister {
  def this(registerData: Array[Long]) =
    this(
      profitSharingPercentage = registerData(0),
      serviceFeeAmount = registerData(1)
    )

  def toRegister: ErgoValue[Coll[java.lang.Long]] = {
    val register: Array[java.lang.Long] = new Array[java.lang.Long](2)

    register(0) = profitSharingPercentage
    register(1) = serviceFeeAmount

    ergoValueOf(register)
  }
}
