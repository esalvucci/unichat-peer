package utility

import java.net.NetworkInterface

import com.typesafe.config.{Config, ConfigFactory}

/** facility for client networking */
object Network {

  val defaultPort = 2552

  /** get the address of this client as agreed in the protocol */
  def defaultAddress: String = lanAddress

  /** get the local address of the machine */
  def lanAddress: String = {
    NetworkInterface.getNetworkInterfaces
      .nextElement.getInterfaceAddresses
      .get(0).getAddress.toString
      .replace("/", "")
  }

  def config: Config = {
    ConfigFactory.parseString("akka.remote.netty.tcp.hostname = \"127.0.0.2\"")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=2556"))
      .withFallback(ConfigFactory.load())
  }

  def address: String = {
    val localHostname = ConfigFactory.load().getString("akka.remote.netty.tcp.hostname")
    val localPort = ConfigFactory.load().getString("akka.remote.netty.tcp.port")
    s"akka.tcp://unichat-system@$localHostname:$localPort/user/messenger-actor/"
  }

}