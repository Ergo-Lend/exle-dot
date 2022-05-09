import com.typesafe.config.ConfigFactory
import config.Configs.readKey
import org.ergoplatform.appkit.Address
import play.api.Configuration

object Playground extends App {
  val config = Configuration(ConfigFactory.load("ergo.conf"))
  val serviceOwner: Address = Address.create(readKey("service.owner"))
  println(serviceOwner)
}
