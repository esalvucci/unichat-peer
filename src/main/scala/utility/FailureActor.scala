package utility

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSystem, Identify, PoisonPill, Props, Terminated}
import akka.routing.{ActorSelectionRoutee, BroadcastGroup, RemoveRoutee}
import ui.MessageActor
import user.UserInChat
import utility.FailureActor.Failure

class FailureActor(paths: Seq[String], userInChatActor: ActorRef, router: ActorRef) extends Actor {

  private val identifyId = 1
  println(paths)
  paths.map(a => context.actorSelection(a)).foreach(u => {
    println(u)
    u ! Identify(identifyId)
  })

  override def receive: Receive = {
    case Terminated(actor) =>
      userInChatActor ! Failure(actor.path.name)
      router ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(userInChatActor.path)))
    case ActorIdentity(1, Some(ref)) =>
      context watch ref

  }
}

object FailureActor {
  def props(paths: Seq[String], sender: ActorRef, router: ActorRef): Props =
    Props(new FailureActor(paths, sender, router))

  final case class Failure(username: String)
}

object FailureTest extends App {

  // create the ActorSystem instance
  val system = ActorSystem("FailureActorTest")

  // create the Parent that will create Kenny
  val paths = List(
    "akka.tcp://FailureActorTest@127.0.0.2:2554/user/frank")
  val router = system.actorOf(BroadcastGroup(paths).props, "router")
  val messanger = system.actorOf(MessageActor.props, "message-actor")
  val actorInChat = system.actorOf(UserInChat.props("enry", List.empty, router )(messanger), name = "enry")
  val frankSelection =  system.actorSelection("akka.tcp://FailureActorTest@127.0.0.2:2554/user/frank")

  // lookup kenny, then kill it
  val failureActor = system.actorOf(FailureActor.props(paths, actorInChat, router), name = "failure-actor")
  frankSelection !(PoisonPill, ActorRef.noSender)
}