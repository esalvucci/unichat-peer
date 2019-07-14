package unichat.user

import akka.actor.Stash
import CasualOrdering.Matrix

import scala.collection.immutable.Map

trait CausalOrdering extends Stash {

  private var matrix: Matrix = Map.empty
  private var username: Option[String] = None

  protected def _username: Option[String] = username
  protected def username_= (value: String ): Unit = username = Some(value)

  def populateMap(users: Seq[String]): Unit =
    matrix ++
      users.map(f => (f, username.get) -> 0).toMap ++
      users.map(f => (username.get, f) -> 0).toMap


  def sentMessage(): Matrix = {
    val currentMatrix: Matrix = Map.canBuildFrom(matrix).result()
    updateSentMessages()
    currentMatrix
  }

  private def updateSentMessages(): Unit =
    matrix = matrix ++ matrix.filter(pair => pair._1._1 == username.get)
      .map {case ((sender: String, receiver: String), receivedMessages: Int) => ((sender, receiver), receivedMessages + 1)}

  def receiveMessage(user: String, senderMatrix: Matrix, function: () => Unit): Unit = {
    if (eligible(user, senderMatrix)) {
      updateReceivedMessages(user, senderMatrix)
      function()
      unstashAll()
    } else {
      stash()
    }
  }

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

  def removeReferenceOf(user: String): Unit =
    matrix = matrix.filterNot(pair => pair._1._1 == user || pair._1._2 == user)
}

object CasualOrdering {
  type Matrix = Map[(String, String), Int]
}