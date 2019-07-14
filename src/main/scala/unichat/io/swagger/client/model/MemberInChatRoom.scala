package unichat.io.swagger.client.model

/**
 * @param username  for example: '''name.surnameN'''
 * @param link  for example: '''akka://system@ip:port/user/message-actor/chatroom/username'''
 */
case class MemberInChatRoom(
  username: Option[String],
  link: Option[String]
)
