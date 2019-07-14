package unichat.user

object ChatMessages {
    final case class JoinedUserMessage(userPath: String)

    final case class UnJoinedUserMessage(userPath: String)
}
