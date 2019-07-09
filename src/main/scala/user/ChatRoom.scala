package user

import akka.actor.Status.Failure
import akka.routing.{ActorSelectionRoutee, AddRoutee, BroadcastGroup}
import ui.MessageActor.ShowWelcomeMessage
import user.ChatRoom.{Exit, JoinInChatRoom}
import utility.ExtendedRouter
import akka.actor.{Actor, ActorRef, Props}
import akka.remote.artery.FlushOnShutdown.Timeout
import com.typesafe.config.ConfigFactory
import io.swagger.client.api.UsernameApi
import io.swagger.client.core.ApiInvoker
import server.WhitePages.{JoinedUserMessage, PutUserChatRoom, ReplyUsersInChat, UnJoinedUserMessage}
import akka.pattern.{ask, pipe}
import io.swagger.client.model.{ListOfUsers, User}

import scala.concurrent.ExecutionContext

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  /*private val whitePages = context.actorSelection("akka.tcp://white-pages-system@127.0.0.2:2553/user/white-pages")
*/
  implicit val executionContext: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(3)
  private val failureActorName = "router-actor"
  private var failureActorOption: Option[ActorRef] = None
  private val userApi = UsernameApi
  private val apiInvoker = ApiInvoker()(context.system)

  override def receive: Receive = {
    case JoinInChatRoom(chatRoom) =>
      //TODO find LAN address of local user
      val localUserAddress = userAddress(chatRoom)
/*      whitePages ! PutUserChatRoom(localUserAddress, chatRoom)*/
      val requestResult = userApi.addUserInChatRoom(chatRoom)
      apiInvoker.execute(requestResult).pipeTo(self)

    case ListOfUsers(listOfUsers: Option[Seq[User]]) =>
      val userInChatActor = context.actorOf(UserInChat.props(username,getLinkFrom(listOfUsers).map(u => extractUsernameFrom(u)), messenger), name = username)
      val failureActor = context.actorOf(ExtendedRouter.props(getLinkFrom(listOfUsers), userInChatActor), name = failureActorName)
      failureActorOption = Some(failureActor)
      messenger ! ShowWelcomeMessage(username, getChatFrom(listOfUsers), userInChatActor)

    case Exit(chatRoom: String, username: String) =>
      val requestResult = userApi.removeUserFromChatRoom(chatRoom, username)
      apiInvoker.execute(requestResult).pipeTo(self)

    case Failure(status) => // ToDo send message to MessageActor

    case JoinedUserMessage(userPath) => failureActorOption.get ! JoinedUserMessage(userPath)
    case UnJoinedUserMessage(userPath) => failureActorOption.get ! UnJoinedUserMessage(userPath)
  }

  private def getChatFrom(users: Option[Seq[User]]): String = {
    val links: Array[String] = getLinkFrom(users).head.split("/")
    links(links.length - 2)
  }
  private def getLinkFrom(users: Option[Seq[User]]): Seq[String] = users match {
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
  final case class Exit(chatRoom: String, username: String)

}
