package utility

import java.util.concurrent.TimeUnit

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, Props, Terminated}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import user.{ChatRoom, UserInChat}

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class FailureActorSpec extends TestKit(ActorSystem(
  "FailureActorSpec",
  ConfigFactory.parseString("""
    akka {
      loglevel = "WARNING"
    }
    """)))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private val paths = List("akka.tcp://unichat-system@127.0.0.2:2552/user/messenger-actor/uni/francesco")
  private val username = "francesco"
  private val chatRoomActor = system.actorOf(ChatRoom.props)

  override def afterAll {
    shutdown()
  }

  "A ChatRoomActor" should {
    "Respond with the same message it receives" in {
      within(FiniteDuration(500, TimeUnit.MILLISECONDS)) {

        expectMsg(Terminated)
      }
    }
  }

}

