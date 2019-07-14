package user

object ChatMessages {
    final case class JoinMe(userPath: String)
    final case class JoinedUserMessage(userPath: String)
    final case class UnJoinedUserMessage(userPath: String)
}
