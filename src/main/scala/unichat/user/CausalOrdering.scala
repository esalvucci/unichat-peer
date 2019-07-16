package unichat.user

import akka.actor.Stash
import CasualOrdering.Matrix

import scala.collection.immutable.Map

/**
  * Trait that manage the ordering of the messages sent by an user according to the happened-before causal relationship.
  */
trait CausalOrdering extends Stash {

  private var matrix: Matrix = Map.empty
  private var username: Option[String] = None

  protected def _username: Option[String] = username
  protected def username_= (value: String ): Unit = username = Some(value)

  /**
    * Populate the matrix of the users (identified by their usernames) and the sent messages.
    * @param usernames The usernames of a chatroom
    */
  def populateMatrix(usernames: Seq[String]): Unit =
    matrix ++
      usernames.map(f => (f, username.get) -> 0).toMap ++
      usernames.map(f => (username.get, f) -> 0).toMap

  /**
    * @return The matrix of the sent messages.
    */
  def sentMessage(): Matrix = {
    val currentMatrix: Matrix = Map.canBuildFrom(matrix).result()
    updateSentMessages()
    currentMatrix
  }

  /**
    * Manage a message naming it eligible or not.
    * @param username The username of a user.
    * @param senderMatrix The matrix of the sender of the message
    * @param function
    */
  def receiveMessage(username: String, senderMatrix: Matrix, function: () => Unit): Unit = {
    if (eligible(username, senderMatrix)) {
      updateReceivedMessages(username, senderMatrix)
      function()
      unstashAll()
    } else {
      stash()
    }
  }

  /**
    * Remove a user from the matrix in case of exit (for all reason) from a chatroom.
    * @param username
    */
  def removeReferenceOf(username: String): Unit =
    matrix = matrix.filterNot(pair => pair._1._1 == username || pair._1._2 == username)

  private def updateSentMessages(): Unit =
    matrix = matrix ++ matrix.filter(pair => pair._1._1 == username.get)
      .map {case ((sender: String, receiver: String), receivedMessages: Int) => ((sender, receiver), receivedMessages + 1)}

  private def eligible(remote: String, senderMatrix: Matrix): Boolean = {
    val receivedMessageInLocal = extractColumnOf(username.get, matrix, remote)
    val receivedMessageBySender = extractColumnOf(remote, senderMatrix, username.get)
    val differences = complementOfIntersection(receivedMessageInLocal, receivedMessageBySender)

    differences.forall {
      case (sender: String, _: Int) =>
        matrix.getOrElse((sender, username.get), 0) >= senderMatrix.getOrElse((sender, remote), 0)
    }
  }

  private def extractColumnOf(username: String, matrix: Matrix, remote: String): Map[String, Int] = {
    matrix.filter(entry => entry._1._1 != remote && entry._1._2 == username)
      .map { case ((sender: String, _: String), receivedMessages: Int) => sender -> receivedMessages }
  }

  private def complementOfIntersection(map: Map[String, Int], map2: Map[String, Int]) =
    map.toSet.diff(map2.toSet) ++ map2.toSet.diff(map.toSet)

  private def updateReceivedMessages(user: String, senderMatrix: Matrix): Unit = {
    val updatedMessages = matrix.getOrElse((user, username.get), 0) + 1
    matrix = matrix + ((user, username.get) -> updatedMessages)
    matrix.map { case ((sender: String, receiver: String), receivedMessages: Int) =>
      val remoteMessages = senderMatrix.getOrElse((sender, receiver), 0)
      if (remoteMessages > receivedMessages) remoteMessages
    }
  }
}

object CasualOrdering {
  type Matrix = Map[(String, String), Int]
}