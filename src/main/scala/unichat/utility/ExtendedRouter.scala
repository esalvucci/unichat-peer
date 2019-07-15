package unichat.utility

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}
import akka.routing._
import unichat.user.ChatMessages.{JoinedUser, UnJoinedUser}
import ExtendedRouter.{Failure, JoinMe, UserExit}

import scala.collection.immutable.Iterable

class ExtendedRouter(paths: Iterable[String], userInChatActor: ActorRef) extends Actor {

  private val router: ActorRef = context.actorOf(BroadcastGroup(paths).props, "router")
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
      userInChatActor ! Failure(actor.path.name)
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userInChatActor.path)))

    case ActorIdentity(1, Some(ref)) =>
      context watch ref

  }

  private def identifyRoutees(): Unit =
    paths.map(a => context.actorSelection(a)).foreach(u => {
      u ! Identify(identifyId)
    })
}

object ExtendedRouter {

  val extendedRouterName = "extended-router-actor"

  def props(paths: Iterable[String], userInChat: ActorRef): Props =
    Props(new ExtendedRouter(paths, userInChat))

  final case class JoinMe(actorPath: String)

  final case class Failure(username: String)

  final case class UserExit(path: String)

}