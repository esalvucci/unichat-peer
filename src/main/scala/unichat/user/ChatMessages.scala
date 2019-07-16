package unichat.user

/**
  * Object which expose messages received by more than an actor.
  */
object ChatMessages {
    final case class JoinedUser(userPath: String)

    final case class UnJoinedUser(userPath: String)
}
