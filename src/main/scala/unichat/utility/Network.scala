package unichat.utility

import java.net.NetworkInterface

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Facility for client networking
  */
object Network {

  val defaultPort = 2552

  /**
    * Get the address of this client as agreed in the protocol
    */
  def defaultAddress: String = lanAddress

  /**
    * Get the local address of the machine
    */
  def lanAddress: String = {
    NetworkInterface.getNetworkInterfaces
      .nextElement.getInterfaceAddresses
      .get(0).getAddress.toString
      .replace("/", "")
  }
}