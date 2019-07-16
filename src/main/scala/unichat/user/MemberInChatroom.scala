package unichat.user

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.Broadcast
import CasualOrdering.Matrix
import ChatMessages.{JoinedUser, UnJoinedUser}
import unichat.ui.MessageHandler.ShowMessage
import unichat.utility.ExtendedRouter._

/**
  * Actor representing the user (member) joined in a specific chatroom.
  * @param localUsername The user's username.
  * @param remoteMembersLinks The remote links of the other users in a chatroom.
  * @param messageHandlerActor The actor which manages the messages sent by a user.
  */
private class MemberInChatroom(localUsername: String, remoteMembersLinks: Seq[String], messageHandlerActor: ActorRef)
  extends Actor with CausalOrdering {

  import MemberInChatroom._

  private val pathSeparator = "/"
  private val extendedRouterActor = context.actorSelection(context.self.path.parent + pathSeparator + extendedRouterName)

  username_=(localUsername)
  populateMatrix(remoteMembersLinks)

  override def receive: Receive = {
    case MessageInChat(content) =>
      val matrix = sentMessage()
      extendedRouterActor ! Broadcast(BroadcastMessage(content, _username.get, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messageHandlerActor ! ShowMessage(content, user))

    case JoinedUser(actorPath) =>
      extendedRouterActor ! JoinedUser(actorPath)

    case unJoin@UnJoinedUser(path) =>
      removeReferenceOf(path)
      extendedRouterActor ! unJoin

    case TerminatedActorNotification(suspicious, actorRef) =>
      extendedRouterActor ! TerminatedActorNotification(suspicious, actorRef)

    case Failure(userInFailure) => removeReferenceOf(userInFailure)

  }
}

/**
  * Companion object for the MemberInChatroom class
  */
object MemberInChatroom {

  def props(username: String, paths: Seq[String], messenger: ActorRef): Props =
    Props(new MemberInChatroom(username, paths, messenger))

  final case class Failure(username: String)

  final case class MessageInChat(content: String)

  final case class BroadcastMessage(content: String, username: String, matrix: Matrix)

}