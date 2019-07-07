package user

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.routing.{ActorSelectionRoutee, AddRoutee, Broadcast, RemoveRoutee}
import server.WhitePages.UnJoinedUserMessage
import ui.MessageActor.{ShowMessage, TestMessage}
import user.CasualOrdering.Matrix

private class UserInChat(localUsername: String, paths: Seq[String], messenger: ActorRef) extends Actor with CausalOrdering with Stash {
  import UserInChat._

  override var username: String = localUsername
  private val routerActorName = "router-actor"
  private val routerActor = context.actorSelection(context.self.path.parent + "/" + routerActorName)
  populateMap(paths)

  override def receive: Receive = {
    case MessageInChat(content) =>

      val matrix = sentMessage()
      routerActor ! Broadcast(BroadcastMessage(content, username, matrix))

    case BroadcastMessage(content, user, senderMatrix) =>
      receiveMessage(user, senderMatrix, () => messenger ! ShowMessage(content, user))

    case UnJoinedUserMessage(user) => removeReferenceOf(user)
    case Failure(userInFailure) => removeReferenceOf(userInFailure)
/*
    // TODO Uncomment for debug
    case TestMessage(userAsReceiver, content) =>
      val azzu = "akka.tcp://unichat-system@127.0.0.2:2554/user/messenger-actor/uni/azzu"
      val frank = "akka.tcp://unichat-system@127.0.0.2:2555/user/messenger-actor/uni/frank"
      val toBeRemoved = if (azzu.contains(userAsReceiver)) frank else azzu
      val toSend = if (azzu.contains(userAsReceiver)) azzu else frank

      context.parent ! RemoveRoutee(ActorSelectionRoutee(context.actorSelection(toBeRemoved)))
      // sentMessage()
      val matrix = sentMessage()
      context.actorSelection(toSend) ! BroadcastMessage(content, username, matrix)
      context.parent ! AddRoutee(ActorSelectionRoutee(context.actorSelection(toSend)))
*/
  }

}

object UserInChat {
  def props(username: String, paths: Seq[String], messenger: ActorRef): Props =
    Props(new UserInChat(username, paths, messenger))

  final case class Failure(username: String)
  final case class MessageInChat(content: String)
  final case class BroadcastMessage(content: String, username: String, matrix: Matrix)
}