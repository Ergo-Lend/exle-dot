import core.SingleLender.Ergs.LendBoxExplorer
import node.Client

object Playground extends App {
//  val config = Configuration(ConfigFactory.load("ergo.conf"))
//  val serviceOwner: Address = Address.create(readKey("service.owner"))
//  println(serviceOwner)
  val client: Client = new Client()
  client.setClient()
  val lendBoxExplorer: LendBoxExplorer = new LendBoxExplorer(client)
  try {
    val serviceBox = lendBoxExplorer.getServiceBox
    println(serviceBox)
  } catch {
    case e: Throwable => throw e
  }
}
