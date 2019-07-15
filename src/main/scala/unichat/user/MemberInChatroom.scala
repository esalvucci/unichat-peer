package unichat.user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.Broadcast
import CasualOrdering.Matrix
import ChatMessages.{JoinedUser, UnJoinedUser}
import unichat.ui.MessageHandler.ShowMessage
import unichat.utility.ExtendedRouter._

private class MemberInChatroom(localUsername: String, paths: Seq[String], messenger: ActorRef)
  extends Actor with CausalOrdering with Stash {

  import MemberInChatroom._

  private val pathSeparator = "/"
  private val extendedRouterActor = context.actorSelection(context.self.path.parent + pathSeparator + extendedRouterName)

  username_=(localUsername)
  populateMap(paths)

  override def receive: Receive = {
    case MessageInChat(content) =>

      val matrix = sentMessage()
      extendedRouterActor ! Broadcast(BroadcastMessage(content, _username.get, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messenger ! ShowMessage(content, user))

    case JoinedUser(actorPath) =>
      extendedRouterActor ! JoinedUser(actorPath)

    case UnJoinedUser(userPath) =>
      removeReferenceOf(userPath)
      extendedRouterActor ! UnJoinedUser(userPath)

    case Failure(userInFailure) => removeReferenceOf(userInFailure)

    case UserExit(path) => extendedRouterActor ! UnJoinedUser(path)
  }
}

object MemberInChatroom {
  def props(username: String, paths: Seq[String], messenger: ActorRef): Props =
    Props(new MemberInChatroom(username, paths, messenger))

  final case class Failure(username: String)

  final case class MessageInChat(content: String)

  final case class BroadcastMessage(content: String, username: String, matrix: Matrix)

}