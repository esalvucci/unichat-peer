package unichat

import akka.actor.ActorSystem
import unichat.ui.MessageActor

object UniChatApp extends App {
  val messageActorName = "messenger-actor"
  val actorSystemName = "unichat-system"

  val whitePagesActorSystemName = "white-pages-system"
  val whitePagesName = "white-pages"

  val actorSystem = ActorSystem.create(actorSystemName)
  actorSystem.actorOf(MessageActor.props, messageActorName)
}
