package unichat.utility

import akka.actor.{ActorSystem, PoisonPill}
import unichat.ui.MessageHandler

object FailureDetectionTest extends App {

  val system = ActorSystem("FailureActorTest")
  val firstMessageHandler = system.actorOf(MessageHandler.props, "first-message-handler")
/*
  val secondMessageHandler = system.actorOf(MessageHandler.props, "second-message-handler")
  val thirdMessageHandler = system.actorOf(MessageHandler.props, "third-message-handler")
*/

  firstMessageHandler ! "firstUser@test"
/*
  secondMessageHandler ! "secondUser@test"
  thirdMessageHandler ! "thirdUser@test"
*/

  firstMessageHandler ! PoisonPill

}