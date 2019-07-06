package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.Broadcast
import ui.MessageActor.ShowMessage

private class UserInChat(localUsername: String, paths: Seq[String], router: ActorRef, messenger: ActorRef) extends Actor with CausalOrdering with Stash {
  import UserInChat._

  override var username: String = localUsername
  populateMap(paths)

  override def receive: Receive = {
    case MessageInChat(content) =>
      val matrix = sentMessage()
      router ! Broadcast(BroadcastMessage(content, username, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messenger ! ShowMessage(content, user))

    case Failure(userInFailure) => removeReferenceTo(userInFailure)

/* Uncomment for debug
    case TestMessage(userAsReceiver, content) =>
      val optionUser = paths.find(user => user.endsWith(userAsReceiver))
      if(optionUser.isDefined) {
        updateSentMessages()
        context.actorSelection(optionUser.get) ! BroadcastMessage(content, username, matrix)
      }
*/
  }

}

object UserInChat {
  def props(username: String, paths: Seq[String], router: ActorRef, messenger: ActorRef): Props =
    Props(new UserInChat(username, paths, router, messenger))

  final case class Failure(username: String)
  final case class MessageInChat(content: String)
  final case class BroadcastMessage(content: String, username: String, matrix: Map[(String, String), Int])
}