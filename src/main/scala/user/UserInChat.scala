package user

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.Broadcast

private class UserInChat(username: String, router: ActorRef, messenger: ActorRef) extends Actor {
  import UserInChat._

  override def receive: Receive = {
    case MessageInChat(content) => router ! Broadcast(content)
    case content: String => println(s"received message: $content")
  }

}

object UserInChat {
  def props(username: String, router: ActorRef, messenger: ActorRef): Props =
    Props(new UserInChat(username, router, messenger))

  final case class MessageInChat(content: String)
}