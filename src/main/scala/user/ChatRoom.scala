package user

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import io.swagger.client.api.MemberInChatRoomApi
import io.swagger.client.model.MemberInChatRoom
import ui.MessageActor.{ShowExitMessage, ShowWelcomeMessage}
import user.ChatMessages.{JoinMe, JoinedUserMessage, UnJoinedUserMessage}
import user.ChatRoom.{Exit, JoinInChatRoom}
import utility.ExtendedRouter.UserExit
import utility.{ExtendedRouter, RemoteAddressExtension}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  private implicit val executionContext: ExecutionContext = context.dispatcher
  private val failureActorName = "router-actor"
  private val userApi = new MemberInChatRoomApi()
  private var failureActorOption: Option[ActorRef] = None
  private var chatroomName: Option[String] = None

  override def receive: Receive = {
    case JoinInChatRoom(chatRoom) =>
      chatroomName = Some(chatRoom)
      val requestResult = userApi.addUserInChatRoomAsync(chatRoom, Some(MemberInChatRoom(Some(username), Some(userAddress))))

      requestResult onComplete {
        case Success(listOfMemberInChatRoom: Seq[MemberInChatRoom]) => self ! listOfMemberInChatRoom
        case Failure(exception) => println(s"Exception: ${exception.getMessage}")
      }

    case members: Seq[MemberInChatRoom] =>
      val userInChatActor = context.actorOf(UserInChat.props(username, getLinkFrom(members).map(u => extractUsernameFrom(u)), messenger), name = username)
      val extendedRouterActor = context.actorOf(ExtendedRouter.props(getLinkFrom(members), userInChatActor), name = failureActorName)
      failureActorOption = Some(extendedRouterActor)
      messenger ! ShowWelcomeMessage(username, chatroomName.get, userInChatActor)
      extendedRouterActor ! JoinMe(userAddress)

    case Exit =>
      userApi.removeUserFromChatRoomAsync(chatroomName.get, username)
      failureActorOption.get ! UserExit(userAddress)
      messenger ! ShowExitMessage(chatroomName.get, username)
      self ! PoisonPill

    case JoinedUserMessage(userPath) => failureActorOption.get ! JoinedUserMessage(userPath)
    case UnJoinedUserMessage(userPath) => failureActorOption.get ! UnJoinedUserMessage(userPath)
  }

  private def getLinkFrom(users: Seq[MemberInChatRoom]): Seq[String] = users.map(_.link).map(_.get)

  private def extractUsernameFrom(user: String) = user.substring(user.lastIndexOf("/") + 1)

  private def userAddress: String = {
    val pathSeparator: String = "/"
    RemoteAddressExtension.get(context.system).address.toString concat self.path.toStringWithoutAddress concat pathSeparator concat username
  }
}

object ChatRoom {
  def props(username: String, messenger: ActorRef): Props = Props(new ChatRoom(username, messenger))

  final case class JoinInChatRoom(chatRoom: String)

  final case class Exit(chatRoom: String)

  final object Exit

}
