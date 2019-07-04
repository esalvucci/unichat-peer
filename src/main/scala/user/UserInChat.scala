package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.Broadcast
import ui.MessageActor.ShowMessage
import user.ChatRoom.Failure

private class UserInChat(username: String, router: ActorRef, messenger: ActorRef) extends Actor with Stash {
  import UserInChat._

  private val users: Seq[String] = Seq.empty
  private var matrix: Map[(String, String), Int] = Map.empty

  override def receive: Receive = {
    case MessageInChat(content) =>
      updateSentMessages()
      router ! Broadcast(BroadcastMessage(content, username, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      if (eligible(senderMatrix)) {
        updateReceivedMessages(user)
        messenger ! ShowMessage(content, user)
        unstashAll()
      } else {
        stash()
      }
      messenger ! ShowMessage(content, user)

    case Failure(userInFailure) =>
      matrix = matrix.filterNot(pair => pair._1._1 == userInFailure || pair._1._2 == userInFailure)
  }

  private def eligible(senderMatrix: Map[(String, String), Int]): Boolean = {
    matrix.filter(pair => pair._1._2 == username)
      .forall{ case ((u: String, u1: String), n: Int) => n >= senderMatrix.getOrElse((u, u1), 0) }
  }

  private def updateReceivedMessages(user: String): Unit = {
    val updatedMessages = matrix.getOrElse((user, username), 0) + 1
    matrix = matrix + ((user, username) ->  updatedMessages)
  }

  private def updateSentMessages(): Unit = {
    val toUpdate = matrix.filter(pair => pair._1._1 == username)
      .map { case ((u: String, u1: String), n: Int) => ((u, u1), n + 1) }

    matrix = matrix ++ toUpdate
  }

}

object UserInChat {
  def props(username: String, router: ActorRef, messenger: ActorRef): Props =
    Props(new UserInChat(username, router, messenger))

  final case class MessageInChat(content: String)
  final case class BroadcastMessage(content: String, username: String, matrix: Map[(String, String), Int])
}