package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.Broadcast
import server.WhitePages.{JoinedUserMessage, UnJoinedUserMessage}
import ui.MessageActor.ShowMessage
import user.CasualOrdering.Matrix
import utility.ExtendedRouter.UserExit

private class UserInChat(localUsername: String, paths: Seq[String], messenger: ActorRef) extends Actor with CausalOrdering with Stash {

  import UserInChat._

  private val extendedRouterName = "router-actor"
  private val extendedRouterActor = context.actorSelection(context.self.path.parent + "/" + extendedRouterName)
  override var username: String = localUsername
  populateMap(paths)

  override def receive: Receive = {
    case MessageInChat(content) =>

      val matrix = sentMessage()
      extendedRouterActor ! Broadcast(BroadcastMessage(content, username, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messenger ! ShowMessage(content, user))

    case JoinedUserMessage(actorPath) =>
      extendedRouterActor ! JoinedUserMessage(actorPath)

    case UnJoinedUserMessage(user) => removeReferenceOf(user)

    case Failure(userInFailure) => removeReferenceOf(userInFailure)

    case UserExit(path) => extendedRouterActor ! UnJoinedUserMessage(path)
  }

}

object UserInChat {
  def props(username: String, paths: Seq[String], messenger: ActorRef): Props =
    Props(new UserInChat(username, paths, messenger))

  final case class Failure(username: String)

  final case class MessageInChat(content: String)

  final case class BroadcastMessage(content: String, username: String, matrix: Matrix)

}