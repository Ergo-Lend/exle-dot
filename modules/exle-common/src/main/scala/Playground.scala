import com.typesafe.config.ConfigFactory
import commons.configs.Configs.readKey
import org.ergoplatform.appkit.Address
import play.api.Configuration

object Playground extends App {
  val config = Configuration(ConfigFactory.load())
  System.out.println(config.get[String]("service.feeAddress"))
}
