package unichat.user

object ChatMessages {
    final case class JoinedUser(userPath: String)

    final case class UnJoinedUser(userPath: String)
}
