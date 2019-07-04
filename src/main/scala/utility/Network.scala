package utility

import java.net.NetworkInterface
import java.util

/** facility for client networking */
object Network {

  val defaultPort = 2552

  /** get the address of this client as agreed in the protocol */
  def defaultAddress: String = lanAddress

  /** get the local address of the machine */
  def lanAddress: String = {
    NetworkInterface.getNetworkInterfaces.nextElement()
      .getInterfaceAddresses.get(1)
      .getAddress
      .toString.replace("/", "")
  }

}