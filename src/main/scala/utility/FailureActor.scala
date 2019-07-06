package utility

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, Identify, Props, Terminated}
import akka.routing.{ActorRefRoutee, RemoveRoutee, Routees}
import akka.util.Timeout
import utility.FailureActor.Failure

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class FailureActor(usersPaths: Seq[String], userInChatActor: ActorRef) extends Actor {
  private implicit val timeout: Timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))
  private implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val pathSeparator = "/"

  private val routerActorName = "router"
  private var router: Option[ActorRef] = None
  context.actorSelection("/user/" + routerActorName) ! Identify

  override def receive: Receive = {
    case ActorIdentity("identifyId", Some(ref)) =>
      router = Some(ref)
      context.watch(router.get)
    case Terminated(actor) =>
      userInChatActor ! Failure(actor.path.name)
      router.get ! RemoveRoutee(ActorRefRoutee(actor))

  }

  private def getUsernameFrom(path: String) = path.split(pathSeparator).tail
}

object FailureActor {
  def props(usersPaths: Seq[String], sender: ActorRef): Props =
    Props(new FailureActor(usersPaths, sender))

  final case class Failure(username: String)
}
