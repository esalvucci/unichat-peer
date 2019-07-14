package utility

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}
import akka.routing._
import user.ChatMessages.{JoinMe, JoinedUserMessage, UnJoinedUserMessage}
import user.UserInChat.BroadcastMessage
import utility.ExtendedRouter.{Failure, UserExit}

import scala.collection.immutable.Iterable

class ExtendedRouter(paths: Iterable[String], userInChatActor: ActorRef) extends Actor {

  private val router: ActorRef = context.actorOf(BroadcastGroup(paths).props, "router")
  private val identifyId = 1

  watchRoutees()

  override def receive: Receive = {
    case JoinedUserMessage(userPath) =>
      val remoteActor = context.actorSelection(userPath)
      router ! AddRoutee(ActorSelectionRoutee(remoteActor))
      remoteActor ! Identify(identifyId)
    case UnJoinedUserMessage(userPath) =>
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userPath)))
    case Broadcast(BroadcastMessage(content, username, matrix)) =>
      router ! Broadcast(BroadcastMessage(content, username, matrix))
    case JoinMe(actorPath) =>
      router ! Broadcast(JoinedUserMessage(actorPath))

    case Terminated(actor) =>
      userInChatActor ! Failure(actor.path.name)
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userInChatActor.path)))
    case ActorIdentity(1, Some(ref)) =>
      context watch ref
    case message: UserExit => router ! Broadcast(message)
  }

  private def watchRoutees(): Unit =
    paths.map(a => context.actorSelection(a)).foreach(u => {
      u ! Identify(identifyId)
    })
}

object ExtendedRouter {
  def props(paths: Iterable[String], userInChat: ActorRef): Props =
    Props(new ExtendedRouter(paths, userInChat))

  final case class Failure(username: String)

  final case class UserExit(path: String)

}