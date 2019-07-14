package unichat.ui

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import unichat.user.ChatRoom
import unichat.user.ChatRoom.{Exit, JoinInChatRoom}
import unichat.user.MemberInChatroom.MessageInChat

import scala.concurrent.Future

private class MessageActor extends Actor with ActorLogging {

  import MessageActor._

  implicit val materializer: Materializer = ActorMaterializer()
  private val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)
  private var memberInChatrooms: Map[String, ActorRef] = Map.empty

  stdinSource.map(text => text.utf8String).runForeach(sendTypedText)

  override def receive: Receive = {
    case text: String if text.contains("@") && text.contains(":") =>
      val colonIndex = text.indexOf(":")
      val receiver = memberInChatrooms.get(text.substring(1, colonIndex))
      if (receiver.isDefined) receiver.get ! MessageInChat(text.substring(colonIndex + 1))
      else showErrorMessage("Insert the correct chat room name")

    case text: String if text.contains("@exit") =>
      val chatRoomName = text.split("@").head
      val chatRoomPath = memberInChatrooms(chatRoomName).path.parent
      val chatRoomActor = context.actorSelection(chatRoomPath)
      memberInChatrooms = memberInChatrooms.filterNot(user => user._1 == chatRoomName)
      chatRoomActor ! Exit

    case usernameAndChatRoomName: String if usernameAndChatRoomName.contains("@") =>
      val usernameAndChatroomNameSplit = usernameAndChatRoomName.trim.split("@")
      val chatroom =
        context.actorOf(ChatRoom.props(usernameAndChatroomNameSplit.head, self), usernameAndChatroomNameSplit.tail.head)
      chatroom ! JoinInChatRoom(usernameAndChatroomNameSplit.tail.head)

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

  showJoinMessage()
}

object MessageActor {
  def props: Props = Props(new MessageActor)

  final case class ShowMessage(content: String, sender: String)

  final case class ShowExitMessage(chatroom: String, username: String)

  final case class ShowWelcomeMessage(username: String, chatRoom: String, actor: ActorRef)

  final case class ErrorJoin(errorText: String)
}
