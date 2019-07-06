package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.{Broadcast, BroadcastGroup}
import ui.MessageActor.ShowMessage

private class UserInChat(username: String, paths: Seq[String], messenger: ActorRef) extends Actor with Stash {
  import UserInChat._

  private var matrix: Map[(String, String), Int] = paths.map(f => (f.substring(f.lastIndexOf("/") + 1), username) -> 0).toMap ++ paths.map(f => (username, f.substring(f.lastIndexOf("/") + 1)) -> 0).toMap
  private val router = context.actorOf(BroadcastGroup(paths).props(), "router")

  override def receive: Receive = {
    case MessageInChat(content) =>
      router ! Broadcast(BroadcastMessage(content, username, matrix))
      updateSentMessages()

    case BroadcastMessage(content, user, senderMatrix) =>
      if (eligible(user, senderMatrix)) {
        updateReceivedMessages(user, senderMatrix)
        messenger ! ShowMessage(content, user)
        unstashAll()
      } else {
        stash()
      }

    case Failure(userInFailure) =>
      matrix = matrix.filterNot(pair => pair._1._1 == userInFailure || pair._1._2 == userInFailure)

/* Uncomment for debug
    case TestMessage(userAsReceiver, content) =>
      val optionUser = paths.find(user => user.endsWith(userAsReceiver))
      if(optionUser.isDefined) {
        updateSentMessages()
        context.actorSelection(optionUser.get) ! BroadcastMessage(content, username, matrix)
      }
*/
  }

  private def eligible(remote: String, senderMatrix: Map[(String, String), Int]): Boolean = {
    val receivedMessageInLocal = matrix.filter(pair => pair._1._2 == username && pair._1._1 != remote).map { case ((u: String, _: String), n: Int) => u -> n }
    val receivedMessageBySender = senderMatrix.filter(pair => pair._1._2 == remote && pair._1._1 != username).map { case ((u: String, _: String), n: Int) => u -> n }
    val differences = receivedMessageInLocal.toSet.diff(receivedMessageBySender.toSet) ++ receivedMessageBySender.toSet.diff(receivedMessageInLocal.toSet)

    differences.forall{ case (sender: String, _: Int) => matrix.getOrElse((sender, username), 0) >= senderMatrix.getOrElse((sender, remote), 0) }
  }

  private def updateReceivedMessages(user: String, senderMatrix: Map[(String, String), Int]): Unit = {
    val updatedMessages = matrix.getOrElse((user, username), 0) + 1
    matrix = matrix + ((user, username) ->  updatedMessages)
    matrix.map { case ((u: String, u1: String), n: Int) =>
      val senderValue = senderMatrix.getOrElse((u, u1), 0)
      if (senderValue > n) senderValue
    }
  }

  private def updateSentMessages(): Unit = {
    val toUpdate = matrix.filter(pair => pair._1._1 == username)
      .map { case ((u: String, u1: String), n: Int) => ((u, u1), n + 1) }

    matrix = matrix ++ toUpdate
  }

}

object UserInChat {
  def props(username: String, paths: List[String])(implicit messenger: ActorRef): Props =
    Props(new UserInChat(username, paths, messenger))

  final case class Failure(username: String)
  final case class MessageInChat(content: String)
  final case class BroadcastMessage(content: String, username: String, matrix: Map[(String, String), Int])
}