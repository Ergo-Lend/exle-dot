package commons.contracts

import enumeratum._

import scala.collection.immutable
import scala.io.Source

sealed trait ExleContract extends EnumEntry {
  // Top Folder
  val exleDomain: String = ""
  // Sub Folder
  val exleDomainType: String = ""
  val contractType: ContractType = ContractTypes.None
  val fileExtension: String = ".es"
  val fileName: String = this.toString + fileExtension
  val dirName: String = "ExleContracts"
  lazy val contractScript: String = get()

  def getPath: String =
    List(dirName, exleDomain, exleDomainType, contractType.plural, fileName)
      .filter(_.nonEmpty)
      .mkString("/")

  def get(): String = {
    val getViaPath: () => String = () => {
      val fullPath: String = getPath
      try {
        val contractSource =
          Source.fromResource(fullPath)

        val contractString = contractSource.mkString
        contractSource.close()

        contractString
      } catch {
        case _: NullPointerException =>
          throw new NullPointerException(s"$fullPath not found")
      }
    }

    val contractString: String = getViaPath()

    contractString
  }
}

object ExleContracts extends Enum[ExleContract] {
  val values: immutable.IndexedSeq[ExleContract] = findValues

  /**
    * Finds the Exle Contract and returns it as a string
    * @param exleContracts the specified exle commons.contracts
    * @return
    */
  // ===== SLE [Single Lender SLErgs.Ergs] ===== //
  // SLE Proxy Contracts
  case object SLECreateLendBoxProxyContract extends SLEProxyContract
  case object SLEFundLendBoxProxyContract extends SLEProxyContract
  case object SLEFundRepaymentBoxProxyContract extends SLEProxyContract

  // SLE Box Guard Scripts
  case object SLEServiceBoxGuardScript extends SLEBoxGuardScript
  case object SLELendBoxGuardScript extends SLEBoxGuardScript
  case object SLERepaymentBoxGuardScript extends SLEBoxGuardScript

  // ===== SLT [Single Lender Tokens] ===== //
  // SLT Proxy Contracts
  case object SLTCreateLendBoxProxyContract extends SLTProxyContract
  case object SLTFundLendBoxProxyContract extends SLTProxyContract
//  case object SLTFundRepaymentBoxProxyContract extends SLTProxyContract

  // SLT Box Guard Scripts
//  case object SLTServiceBoxGuardScript extends SLTBoxGuardScript
//  case object SLTLendBoxGuardScript extends SLTBoxGuardScript
//  case object SLTRepaymentBoxGuardScript extends SLTBoxGuardScript

  // ===== Test Assets ===== //
  case object DummyErgoScript extends TestAssetsContract
}

//<editor-fold desc="Contract Domains">
/**
  * ===== Contract Domains Instantiation =====
  */
// Single Lender Tokens
sealed trait SLTContract extends ExleContract {
  override val exleDomain: String = "SingleLender"
  override val exleDomainType: String = "Tokens"
}

// Single Lender SLErgs.Ergs
sealed trait SLEContract extends ExleContract {
  override val exleDomain: String = "SingleLender"
  override val exleDomainType: String = "SLErgs.Ergs"
}
//</editor-fold>

//<editor-fold desc="Detailed Contract Types">
/**
  * // ===== Detailed Level Contracts =====
  */
// Single Lender Tokens
sealed trait SLTProxyContract extends SLTContract {
  override val contractType: ContractType = ContractTypes.ProxyContract
}

sealed trait SLTBoxGuardScript extends SLTContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}

// Single Lender SLErgs.Ergs
sealed trait SLEProxyContract extends SLEContract {
  override val contractType: ContractType = ContractTypes.ProxyContract
}

sealed trait SLEBoxGuardScript extends SLEContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}

// Test Assets
sealed trait TestAssetsContract extends ExleContract {
  override val exleDomain: String = "TestAssets"
}
//</editor-fold>

//<editor-fold desc="Contract Type Enum">
/**
  * Describes the different contract types as Enums
  */
sealed trait ContractType extends EnumEntry { val plural: String }

object ContractTypes extends Enum[ContractType] {
  val values: immutable.IndexedSeq[ContractType] = findValues

  case object ProxyContract extends ContractType {
    override val plural = "ProxyContracts"
  }

  case object BoxGuardScript extends ContractType {
    override val plural = "BoxGuardScripts"
  }
  case object None extends ContractType { override val plural = "" }
}
//</editor-fold>
