package unichat.utility

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}
import akka.routing._
import unichat.user.ChatMessages.{JoinedUser, UnJoinedUser}
import ExtendedRouter.{Failure, JoinMe, TerminatedActorNotification, UserExit}

import scala.collection.immutable.Iterable

class ExtendedRouter(paths: Iterable[String], userInChatActor: ActorRef) extends Actor {

  private val router: ActorRef = context.actorOf(BroadcastGroup(paths).props, "router")
  private val identifyId = 1
  private var suspiciousActors: Set[ActorRef] = Set.empty

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

    case TerminatedActorNotification(suspicious, actorRef: ActorRef) =>
      if (suspiciousActors != suspicious) {
        suspiciousActors = suspiciousActors.filterNot(_ == sender) ++ suspicious
      } else {
        userInChatActor ! Failure(actorRef.path.name)
        router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(actorRef.path)))
        suspiciousActors = suspiciousActors.filterNot(_ == actorRef)
      }

    case Terminated(actor) =>
      suspiciousActors += actor
      router ! Broadcast(TerminatedActorNotification(suspiciousActors, actor))

    case ActorIdentity(1, Some(ref)) =>
      context watch ref
      println("I'm watching " + ref)

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

  final case class TerminatedActorNotification(suspicious: Set[ActorRef], actorRef: ActorRef)

  final case class JoinMe(actorPath: String)

  final case class Failure(username: String)

  final case class UserExit(path: String)

}