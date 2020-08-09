package unichat.ui

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import unichat.user.ChatRoom
import unichat.user.ChatRoom.{Exit, JoinInChatRoom}
import unichat.user.MemberInChatroom.MessageInChat

import scala.concurrent.Future

/**
  * Actor which manages the messages sent by a user.
  * It also keep track of the chatrooms joined by a user.
  */
private class MessageHandler extends Actor with ActorLogging {

  import MessageHandler._

  implicit val materializer: Materializer = ActorMaterializer()
  private val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)
  private var memberInChatrooms: Map[String, ActorRef] = Map.empty
  private val joinPattern = "([0-9a-zA-Z]+)@([0-9a-zA-Z]+)".r
  private val exitPattern = "([0-9a-zA-Z]+)@exit".r
  private val messagePattern = "@([0-9a-zA-Z]+):([0-9a-zA-Z,.;|\\s]+)".r

  stdinSource.map(text => text.utf8String).runForeach(sendTypedText)

  override def receive: Receive = {
    case text: String => text match {
      case messagePattern(chatRoom, content) =>
        val receiver = memberInChatrooms.get(chatRoom)
        if (receiver.isDefined) receiver.get ! MessageInChat(content)
        else showErrorMessage("Insert the correct chat room name")

      case t: String => getStringWithoutSpaces(t) match {
        case exitPattern(chatRoom) =>
          val chatRoomPath = memberInChatrooms(chatRoom).path.parent
          val chatRoomActor = context.actorSelection(chatRoomPath)
          memberInChatrooms = memberInChatrooms.filterNot(user => user._1 == chatRoom)
          chatRoomActor ! Exit

        case joinPattern(username, chatRoomName) =>
          val chatRoomActor =
            context.actorOf(ChatRoom.props(username, self), chatRoomName)
          chatRoomActor ! JoinInChatRoom(chatRoomName)
      }

      case _ =>
        showErrorMessage("enter correct data!")
        showJoinMessage()
    }

    case ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef) =>
      memberInChatrooms = memberInChatrooms + (chatRoom -> actor)
      println(s"Welcome in $chatRoom $username")

    case ShowMessage(content, sender) => println(s"$sender: $content")

    case ShowExitMessage(chatRoom, username) => println(s"From chat room $chatRoom: bye bye $username!")

    case ErrorJoin(errorText) =>
      showErrorMessage(errorText)
      showJoinMessage()
  }

  private def showJoinMessage(): Unit =
    println("Enter your username and chat-room name as <username@chatroomname>")

  private def showErrorMessage(error: String): Unit = println(s"Error: $error")

  private def sendTypedText(content: String): Unit = self ! content

  private def getStringWithoutSpaces(text: String) = text.filterNot((x: Char) => x.isWhitespace)

  showJoinMessage()
}

/**
  * Companion object for the MessageHandler class.
  */
object MessageHandler {
  def props: Props = Props(new MessageHandler)

  final case class ShowMessage(content: String, sender: String)

  final case class ShowExitMessage(chatRoom: String, username: String)

  final case class ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef)

  final case class ErrorJoin(errorText: String)
}
