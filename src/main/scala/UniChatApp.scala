import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import server.WhitePages
import ui.MessageActor
import utility.Network

object UniChatApp extends App {
  val messageActorName = "messenger-actor"
  val actorSystemName = "unichat-system"

  val whitePagesActorSystemName = "white-pages-system"
  val whitePagesName = "white-pages"

  val configurationWithAddressAndPort =
    ConfigFactory.parseString("akka.remote.netty.tcp.hostname = \"" + Network.lanAddress + "\"")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + Network.defaultPort))
      .withFallback(ConfigFactory.load())

  // TODO uncomment when resolve error of ip LAN in windows
  val actorSystem = ActorSystem.create(actorSystemName /*, configurationWithAddressAndPort*/)
  actorSystem.actorOf(MessageActor.props, messageActorName)

  // TODO Uncomment to run server
/*
  val whitePagesActorSystem = ActorSystem.create(whitePagesActorSystemName /*, configurationWithAddressAndPort*/)
  whitePagesActorSystem.actorOf(WhitePages.props, whitePagesName)
*/
}
