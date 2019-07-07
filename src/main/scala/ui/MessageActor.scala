package ui

import java.util.regex.Pattern

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import user.ChatRoom
import user.ChatRoom.JoinInChatRoom
import user.UserInChat.MessageInChat

import scala.concurrent.Future
import scala.util.matching.Regex

private class MessageActor extends Actor with ActorLogging {

  import MessageActor._

  implicit val materializer: Materializer = ActorMaterializer()

  private var usersInChat: Map[String, ActorRef] = Map.empty

  private val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)
  stdinSource.map(text => text.utf8String).runForeach(sendTypedText)

  override def receive: Receive = {
    case text: String if text.contains("@") && text.contains(":") =>
      val colonIndex = text.indexOf(":")
      val receiver = usersInChat.get(text.substring(1, colonIndex))
      if (receiver.isDefined) receiver.get ! MessageInChat(text.substring(colonIndex + 1))
      else showErrorMessage("Insert the correct chat room name")

    case usernameAndChatRoomName: String if usernameAndChatRoomName.contains("@") =>
      val usernameAndChatroomNameSplit = usernameAndChatRoomName.trim.split("@")
      val chatroom = context.actorOf(ChatRoom.props(usernameAndChatroomNameSplit.head, self), name = usernameAndChatroomNameSplit.tail.head)
      chatroom ! JoinInChatRoom(usernameAndChatroomNameSplit.tail.head)

    case ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef) =>
      usersInChat = usersInChat + (chatRoom -> actor)
      println(s"Welcome in $chatRoom $username")

    case ShowMessage(content, sender) => println(s"$sender: $content")

    case ErrorJoin(errorText) =>
      showErrorMessage(errorText)
      showJoinMessage()
/*
    // TODO Uncomment only for tester actor for debug
    case text: String if text.startsWith("test:") =>
      val atIndex = text.indexOf("@")
      val colonIndex = text.indexOf(":")
      val username = text.substring(colonIndex + 1, atIndex)
      val content = text.substring(atIndex + 1)
      usersInChat.head._2 ! TestMessage(username, content)
*/
    case t: String =>
      println(new Regex("^[a-zA-Z0-9]+@[a-zA-Z0-9]+$").matches(t))
      showErrorMessage("enter correct data!")
      showJoinMessage()
  }

  private def sendTypedText(content: String): Unit = self ! content

  private def showJoinMessage(): Unit =
    println("Enter your username and chat-room name as <username@chatroomname>")

  private def showErrorMessage(error: String): Unit = println(s"Error: $error")

  showJoinMessage()
}

object MessageActor {
  def props: Props = Props(new MessageActor)

  final case class ShowMessage(content: String, sender: String)
  final case class ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef)
  final case class ErrorJoin(errorText: String)

  final case class TestMessage(username: String, content: String)
}
