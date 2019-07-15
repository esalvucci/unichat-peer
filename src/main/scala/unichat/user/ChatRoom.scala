package unichat.user

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import unichat.io.swagger.client.model.{MemberInChatRoom, MemberInChatRoomApi}
import ChatMessages.{JoinedUser, UnJoinedUser}
import ChatRoom.{Exit, JoinInChatRoom}
import unichat.ui.MessageHandler.{ShowExitMessage, ShowWelcomeMessage}
import unichat.utility.{ExtendedRouter, RemoteAddressExtension}
import unichat.utility.ExtendedRouter.{JoinMe, UserExit}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  private implicit val executionContext: ExecutionContext = context.dispatcher
  private val userApi = new MemberInChatRoomApi()
  private val / = "/"
  private var extendedRouterActor: Option[ActorRef] = None

  override def preStart(): Unit = {
    val addResult = userApi.addUserInChatRoomAsync(chatRoomName, Some(MemberInChatRoom(Some(username), Some(getMemberAddress))))
    addResult onComplete {
      case Success(listOfMembersInChatRoom: Seq[MemberInChatRoom]) => self ! listOfMembersInChatRoom
      case Failure(exception) => println(s"Exception: ${exception.getMessage}")
    }
  }

  override def postStop(): Unit = gracefulStop()

  override def receive: Receive = {
    case members: Seq[MemberInChatRoom] =>
      val memberInChatRoom =
        context.actorOf(MemberInChatroom.props(username, members.map(getUsernameFrom), messenger), name = username)
      val extendedRouterActorRef = context.actorOf(ExtendedRouter.props(members.map(getLinkFrom), memberInChatRoom),
        name = ExtendedRouter.extendedRouterName)
      extendedRouterActor = Some(extendedRouterActorRef)
      messenger ! ShowWelcomeMessage(username, chatRoomName, memberInChatRoom)
      extendedRouterActorRef ! JoinMe(getMemberAddress)

    case Exit =>
      gracefulStop()

  }

  private def gracefulStop(): Unit = {
    userApi.removeUserFromChatRoomAsync(chatRoomName, username)
    extendedRouterActor.get ! UserExit(getMemberAddress)
    messenger ! ShowExitMessage(chatRoomName, username)
    self ! PoisonPill
  }

  private def getLinkFrom(memberInChatRoom: MemberInChatRoom) = memberInChatRoom.link.getOrElse("")

  private def getUsernameFrom(memberInChatRoom: MemberInChatRoom) = {
    val link = getLinkFrom(memberInChatRoom)
    link.substring(link.lastIndexOf(/) + 1)
  }

  private def chatRoomName = context.self.path.name

  private def getMemberAddress: String = {
    RemoteAddressExtension.get(context.system).address.toString concat self.path
      .toStringWithoutAddress concat / concat username
  }
}

object ChatRoom {
  def props(username: String, messenger: ActorRef): Props = Props(new ChatRoom(username, messenger))

  final case class JoinInChatRoom(chatRoom: String)

  final case class Exit(chatRoom: String)

  final object Exit

}
