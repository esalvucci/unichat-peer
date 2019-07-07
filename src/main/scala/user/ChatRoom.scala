package user

import akka.actor.Terminated
import akka.routing.{ActorSelectionRoutee, AddRoutee, BroadcastGroup}
import ui.MessageActor.ShowWelcomeMessage
import user.ChatRoom.JoinInChatRoom
import utility.FailureActor
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.ConfigFactory
import server.WhitePages.{JoinedUserMessage, PutUserChatRoom, ReplyUsersInChat, UnJoinedUserMessage}

private class ChatRoom(username: String, messenger: ActorRef) extends Actor {
  private val whitePages = context.actorSelection("akka.tcp://white-pages-system@127.0.0.2:2553/user/white-pages")

  // TODO encapsulate the router in a custom router actor
  private val router: ActorRef = context.actorOf(BroadcastGroup(Seq()).props, "router")

  private val failureActorName = "failureActor"

  override def receive: Receive = {
    case JoinInChatRoom(chatRoom) =>
      //TODO find LAN address of local user
      val localUserAddress = userAddress(chatRoom)
      whitePages ! PutUserChatRoom(localUserAddress, chatRoom)

    case ReplyUsersInChat(chatRoomName, userPaths) =>
      userPaths.foreach(userPath => router ! AddRoutee(ActorSelectionRoutee(context.actorSelection(userPath))))
      val userInChatActor = context.actorOf(UserInChat.props(username, userPaths.map(extractUsernameFrom), router, messenger), name = username)
      val failureActor = context.actorOf(FailureActor.props(userPaths, userInChatActor), failureActorName)
      messenger ! ShowWelcomeMessage(username, chatRoomName, userInChatActor)

    case JoinedUserMessage(userPath) =>  router ! AddRoutee(ActorSelectionRoutee(context.actorSelection(userPath)))
    case UnJoinedUserMessage(userPath) => // TODO remove user from Router (FailureDetector) and send it to the UserInChat

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
}
