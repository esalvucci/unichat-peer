package utility

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}
import akka.routing.{ActorRefRoutee, ActorSelectionRoutee, AddRoutee, Broadcast, BroadcastGroup, GetRoutees, RemoveRoutee, Routees}
import server.WhitePages.{JoinMe, JoinedUserMessage, UnJoinedUserMessage}
import user.UserInChat.BroadcastMessage
import utility.ExtendedRouter.Failure

import scala.collection.immutable.Iterable
class ExtendedRouter(paths: Iterable[String], userInChatActor: ActorRef) extends Actor {

  private val router: ActorRef = context.actorOf(BroadcastGroup(paths).props, "router")
  private val identifyId = 1

  watchRoutees()

  override def receive: Receive = {
    case JoinedUserMessage(userPath) =>
      val remoteActor = context.actorSelection(userPath)
      router ! AddRoutee(ActorSelectionRoutee(remoteActor))
      router ! GetRoutees
      remoteActor ! Identify(identifyId)
    case UnJoinedUserMessage(userPath) =>
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userPath)))
    case Broadcast(BroadcastMessage(content, username, matrix)) =>
      router ! GetRoutees
      router ! Broadcast(BroadcastMessage(content, username, matrix))
    case Routees(r) => println(r)
    case JoinMe(actorPath) =>
      router ! Broadcast(JoinedUserMessage(actorPath))
/*
    case AddRoutee(routee) => router ! AddRoutee(routee)
*/
    case Terminated(actor) =>
      userInChatActor ! Failure(actor.path.name)
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userInChatActor.path)))
    case ActorIdentity(1, Some(ref)) =>
      context watch ref
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
}