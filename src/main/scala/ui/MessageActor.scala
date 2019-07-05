package ui

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import user.ChatRoom.JoinInChatRoom
import user.ChatRoom
import user.UserInChat.MessageInChat

import scala.concurrent.Future

private class MessageActor extends Actor with ActorLogging {

  import MessageActor._

  implicit val materializer: Materializer = ActorMaterializer()

  private var usersInChat: Option[(String, ActorRef)] = Option.empty

  private val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)
  stdinSource.map(text => text.utf8String).runForeach(sendTypedText)

  override def receive: Receive = joining

  private def sendTypedText(content: String): Unit = self ! content

  private def joining: Receive = {
    case input: String if input.contains("@") =>
      val data = input.trim.split("@")
      context.actorOf(ChatRoom.props, name = data.tail.head) ! JoinInChatRoom(data.head, data.tail.head)

    case JoinedInChat(username: String, chatRoom: String, actor: ActorRef) =>
      usersInChat = Some(chatRoom -> actor)
      println(s"Welcome in $chatRoom $username")
      context.become(joined)

    case ErrorJoin(errorText) =>
      showErrorMessage(errorText)
      showJoinMessage()

    case _ =>
      showErrorMessage("enter correct data!")
      showJoinMessage()
  }

  private def joined: Receive = {
    case ShowMessage(content, sender) => println(s"$sender: $content")

/* Uncomment only for tester actor for debug
    case text: String if text.startsWith("test:") =>
      val username = text.substring(text.indexOf(":") + 1, text.indexOf("@"))
      val content = text.substring(text.indexOf("@") + 1)
      println(username)
      println(content)
      usersInChat.head._2 ! TestMessage(username, content)
*/
    case text: String if usersInChat.nonEmpty => usersInChat.head._2 ! MessageInChat(text)

  }

  private def showJoinMessage(): Unit =
    println("Enter your username and chat-room name as <username@chatroomname>")

  private def showErrorMessage(error: String): Unit = println(s"Error: $error")

  showJoinMessage()
}

object MessageActor {
  def props: Props = Props(new MessageActor)

  final case class ShowMessage(content: String, sender: String)
  final case class JoinedInChat(username: String, chatRoom: String, actor: ActorRef)
  final case class ErrorJoin(errorText: String)

  final case class TestMessage(username: String, content: String)
}
