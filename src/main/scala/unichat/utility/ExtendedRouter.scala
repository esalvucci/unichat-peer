package unichat.utility

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}
import akka.routing._
import unichat.user.ChatMessages.{JoinedUser, UnJoinedUser}
import ExtendedRouter.{Failure, JoinMe, UserExit}

import scala.collection.immutable.Iterable

/**
  * Enhance the akka router (which will sends user's messages in broadcast to other peers).
  * It redirects user's messages to the router itself and adds full Failure Detenction for remote users.
  * @param remoteMembersLinks The paths of the other users joined in a chatroom
  * @param memberInChatRoom The actor representing the user in a chatroom
  */
private class ExtendedRouter(remoteMembersLinks: Iterable[String], memberInChatRoom: ActorRef) extends Actor {

  private val router: ActorRef = context.actorOf(BroadcastGroup(remoteMembersLinks).props, "router")
  private val identifyId = 1

  identifyRoutees()

  override def receive: Receive = {
    case broadcast: Broadcast => router ! broadcast

    case JoinMe(actorPath) =>
      router ! Broadcast(JoinedUser(actorPath))

    case UserExit(path) => router ! Broadcast(UnJoinedUser(path))

    case JoinedUser(userPath) =>
      val remoteActor = context.actorSelection(userPath)
      router ! AddRoutee(ActorSelectionRoutee(remoteActor))
      remoteActor ! Identify(identifyId)

    case UnJoinedUser(userPath) =>
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userPath)))

    case Terminated(actor) =>
      memberInChatRoom ! Failure(actor.path.name)
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(memberInChatRoom.path)))

    case ActorIdentity(1, Some(ref)) =>
      context watch ref

  }

  private def identifyRoutees(): Unit =
    remoteMembersLinks.map(a => context.actorSelection(a)).foreach(u => {
      u ! Identify(identifyId)
    })
}

/**
  * Companion object for the ExtendedRouter class
  */
object ExtendedRouter {

  val extendedRouterName = "extended-router-actor"

  def props(paths: Iterable[String], userInChat: ActorRef): Props =
    Props(new ExtendedRouter(paths, userInChat))

  final case class JoinMe(actorPath: String)

  final case class Failure(username: String)

  final case class UserExit(path: String)

}