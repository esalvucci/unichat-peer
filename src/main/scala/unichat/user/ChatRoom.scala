package unichat.user

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import unichat.io.swagger.client.model.{MemberInChatRoom, MemberInChatRoomApi}
import ChatMessages.{JoinedUserMessage, UnJoinedUserMessage}
import ChatRoom.{Exit, JoinInChatRoom}
import unichat.ui.MessageActor.{ShowExitMessage, ShowWelcomeMessage}
import unichat.utility.{ExtendedRouter, RemoteAddressExtension}
import unichat.utility.ExtendedRouter.{JoinMe, UserExit}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  private implicit val executionContext: ExecutionContext = context.dispatcher
  private val userApi = new MemberInChatRoomApi()
  private val pathSeparator: String = "/"
  private var extendedRouterActor: Option[ActorRef] = None
  private var chatroomName: Option[String] = None

  override def receive: Receive = {
    case JoinInChatRoom(chatRoom) =>
      chatroomName = Some(chatRoom)
      val addResult = userApi.addUserInChatRoomAsync(chatRoom, Some(MemberInChatRoom(Some(username), Some(getMemberAddress))))
      addResult onComplete {
        case Success(listOfMembersInChatRoom: Seq[MemberInChatRoom]) => self ! listOfMembersInChatRoom
        case Failure(exception) => println(s"Exception: ${exception.getMessage}")
      }

    case members: Seq[MemberInChatRoom] =>
      val memberInChatRoom =
        context.actorOf(MemberInChatroom.props(username, members.map(getUsernameFrom), messenger), name = username)
      val extendedRouterActorRef = context.actorOf(ExtendedRouter.props(members.map(getLinkFrom), memberInChatRoom),
        name = ExtendedRouter.extendedRouterName)
      extendedRouterActor = Some(extendedRouterActorRef)
      messenger ! ShowWelcomeMessage(username, chatroomName.get, memberInChatRoom)
      extendedRouterActorRef ! JoinMe(getMemberAddress)

    case Exit =>
      userApi.removeUserFromChatRoomAsync(chatroomName.get, username)
      extendedRouterActor.get ! UserExit(getMemberAddress)
      messenger ! ShowExitMessage(chatroomName.get, username)
      self ! PoisonPill

    case JoinedUserMessage(userPath) =>
      extendedRouterActor.get ! JoinedUserMessage(userPath)

    case UnJoinedUserMessage(userPath) =>
      extendedRouterActor.get ! UnJoinedUserMessage(userPath)
  }

  private def getLinkFrom(memberInChatRoom: MemberInChatRoom) = memberInChatRoom.link.getOrElse("")

  private def getUsernameFrom(memberInChatRoom: MemberInChatRoom) = {
    val link = getLinkFrom(memberInChatRoom)
    link.substring(link.lastIndexOf(pathSeparator) + 1)
  }

  private def getMemberAddress: String = {
    RemoteAddressExtension.get(context.system).address.toString concat self.path
      .toStringWithoutAddress concat pathSeparator concat username
  }
}

object ChatRoom {
  def props(username: String, messenger: ActorRef): Props = Props(new ChatRoom(username, messenger))

  final case class JoinInChatRoom(chatRoom: String)

  final case class Exit(chatRoom: String)

  final object Exit

}
