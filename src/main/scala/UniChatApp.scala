import akka.actor.ActorSystem
import ui.MessageActor

object UniChatApp extends App {
  val system = ActorSystem("unichat-system")
  system.actorOf(MessageActor.props, "messenger-actor")
}