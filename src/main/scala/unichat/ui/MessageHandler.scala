package unichat.ui

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import unichat.user.ChatRoom
import unichat.user.ChatRoom.{Exit, JoinInChatRoom}
import unichat.user.MemberInChatroom.MessageInChat

import scala.concurrent.Future

private class MessageHandler extends Actor with ActorLogging {

  import MessageHandler._

  implicit val materializer: Materializer = ActorMaterializer()
  private val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)
  private var memberInChatrooms: Map[String, ActorRef] = Map.empty
  private val joinPattern = "([0-9a-zA-Z]+)@([0-9a-zA-Z]+)".r
  private val exitPattern = "([0-9a-zA-Z]+)@exit".r
  private val messagePattern = "@([0-9a-zA-Z]+):([0-9a-zA-Z|\\s]+)".r

  stdinSource.map(text => text.utf8String).runForeach(sendTypedText)

  override def receive: Receive = {
    case text: String if messagePattern.findFirstIn(text).isDefined =>
      val messagePattern(chatroom, content) = text
      val receiver = memberInChatrooms.get(chatroom)
      if (receiver.isDefined) receiver.get ! MessageInChat(content)
      else showErrorMessage("Insert the correct chat room name")

    case text: String if exitPattern.findFirstIn(text).isDefined =>
      val exitPattern(chatroom) = getStringWithoutSpaces(text)
      val chatRoomPath = memberInChatrooms(chatroom).path.parent
      val chatRoomActor = context.actorSelection(chatRoomPath)
      memberInChatrooms = memberInChatrooms.filterNot(user => user._1 == chatroom)
      chatRoomActor ! Exit

    case usernameAndChatRoomName: String if joinPattern.findFirstIn(usernameAndChatRoomName).isDefined =>
      val joinPattern(username, chatroomName) = getStringWithoutSpaces(usernameAndChatRoomName)
      val chatroomActor =
        context.actorOf(ChatRoom.props(username, self), chatroomName)
      chatroomActor ! JoinInChatRoom(chatroomName)

    case ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef) =>
      memberInChatrooms = memberInChatrooms + (chatRoom -> actor)
      println(s"Welcome in $chatRoom $username")

    case ShowMessage(content, sender) => println(s"$sender: $content")

    case ShowExitMessage(chatroom, username) => println(s"From chat room $chatroom: bye bye $username!")

    case ErrorJoin(errorText) =>
      showErrorMessage(errorText)
      showJoinMessage()

    case _: String =>
      showErrorMessage("enter correct data!")
      showJoinMessage()
  }

  private def showJoinMessage(): Unit =
    println("Enter your username and chat-room name as <username@chatroomname>")

  private def showErrorMessage(error: String): Unit = println(s"Error: $error")

  private def sendTypedText(content: String): Unit = self ! content

  private def getStringWithoutSpaces(text: String) = text.filterNot((x: Char) => x.isWhitespace)

  showJoinMessage()
}

object MessageHandler {
  def props: Props = Props(new MessageHandler)

  final case class ShowMessage(content: String, sender: String)

  final case class ShowExitMessage(chatroom: String, username: String)

  final case class ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef)

  final case class ErrorJoin(errorText: String)
}
