package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.{ActorRefRoutee, ActorSelectionRoutee, AddRoutee, Broadcast, RemoveRoutee}
import server.WhitePages.{JoinMe, JoinedUserMessage, UnJoinedUserMessage}
import ui.MessageActor.{ShowMessage, TestMessage}
import user.CasualOrdering.Matrix

private class UserInChat(localUsername: String, paths: Seq[String], messenger: ActorRef) extends Actor with CausalOrdering with Stash {
  import UserInChat._

  override var username: String = localUsername
  private val extendedRouterName = "router-actor"
  private val extendedRouterActor = context.actorSelection(context.self.path.parent + "/" + extendedRouterName)
  populateMap(paths)

  override def receive: Receive = {
    case MessageInChat(content) =>
      val matrix = sentMessage()
      extendedRouterActor ! Broadcast(BroadcastMessage(content, username, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messenger ! ShowMessage(content, user))

    case JoinedUserMessage(actorPath) =>
      extendedRouterActor ! JoinedUserMessage(actorPath)

    case UnJoinedUserMessage(user) =>
      removeReferenceOf(user)

    case Failure(userInFailure) =>
      removeReferenceOf(userInFailure)
  }

}

object UserInChat {
  def props(username: String, paths: Seq[String], messenger: ActorRef): Props =
    Props(new UserInChat(username, paths, messenger))

  final case class Failure(username: String)
  final case class MessageInChat(content: String)
  final case class BroadcastMessage(content: String, username: String, matrix: Matrix)
}