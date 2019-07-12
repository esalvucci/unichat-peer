package user

import akka.actor.Status.{Failure, Success}
import ui.MessageActor.ShowWelcomeMessage
import user.ChatRoom.{Exit, JoinInChatRoom}
import utility.ExtendedRouter
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.ConfigFactory
import io.swagger.client.ApiInvoker
import server.WhitePages.{JoinMe, JoinedUserMessage, PutUserChatRoom, ReplyUsersInChat, UnJoinedUserMessage}
import io.swagger.client.model.{ListOfMemberInChatRoom, MemberInChatRoom}
import io.swagger.client.api.MemberInChatRoomApi

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  private implicit val executionContext: ExecutionContext = context.dispatcher
  private val failureActorName = "router-actor"
  private var failureActorOption: Option[ActorRef] = None
  private val userApi = new MemberInChatRoomApi()
  private var chatroomName: Option[String] = None

  override def receive: Receive = {
    case JoinInChatRoom(chatRoom) =>
      //TODO find LAN address of local user
      chatroomName = Some(chatRoom)
      val localUserAddress = userAddress(chatRoom)
/*      whitePages ! PutUserChatRoom(localUserAddress, chatRoom)*/
      val requestResult = userApi.addUserInChatRoomAsync(chatRoom)
      requestResult.map(list => self ! list)

    case ListOfMemberInChatRoom(listOfUsers: Option[Seq[MemberInChatRoom]]) =>
      println(listOfUsers)
      val userInChatActor = context.actorOf(UserInChat.props(username,getLinkFrom(listOfUsers).map(u => extractUsernameFrom(u)), messenger), name = username)
      val extendedRouterActor = context.actorOf(ExtendedRouter.props(getLinkFrom(listOfUsers), userInChatActor), name = failureActorName)
      failureActorOption = Some(extendedRouterActor)
      messenger ! ShowWelcomeMessage(username, chatroomName.get, userInChatActor)
      extendedRouterActor ! JoinMe

    case Exit(chatRoom: String) =>
      userApi.removeUserFromChatRoomAsync(chatRoom, username)

    case Failure(status) => // ToDo send message to MessageActor

    case JoinedUserMessage(userPath) => failureActorOption.get ! JoinedUserMessage(userPath)
    case UnJoinedUserMessage(userPath) => failureActorOption.get ! UnJoinedUserMessage(userPath)
  }

  private def getLinkFrom(users: Option[Seq[MemberInChatRoom]]): Seq[String] = users match {
    case Some(user) => user.map(_.link).map(_.get.link.get)
    case None => Seq.empty
  }
  private def extractUsernameFrom(user: String) = user.substring(user.lastIndexOf("/") + 1)

  private def userAddress(chatRoom: String): String = {
    val localHostname = ConfigFactory.defaultApplication().getString("akka.remote.netty.tcp.hostname")
    val localPort = ConfigFactory.defaultApplication().getString("akka.remote.netty.tcp.port")
    s"akka.tcp://unichat-system@$localHostname:$localPort/user/messenger-actor/$chatRoom/$username"
  }
}

object ChatRoom {
  def props(username: String, messenger: ActorRef): Props = Props(new ChatRoom(username, messenger))

  final case class JoinInChatRoom(chatRoom: String)
  final case class Exit(chatRoom: String)

}
