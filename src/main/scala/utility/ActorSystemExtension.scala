package utility

/** thanks to kaichi for AddressExtension
  *  https://stackoverflow.com/questions/36333282/akka-remote-get-autogenerated-port
  */

import akka.actor.{ActorSystem, Address, ExtendedActorSystem, Extension, ExtensionId}

/** A utility module that provide method to enrich Akka ActorSystem */
object ActorSystemExtension {
  /** An extension for Akka ActorSystem that provides an addresses extractor */
  object AddressExtension extends ExtensionId[AddressExtension] {
    /** Creates the needed extended system in order to work with defined method  */
    override def createExtension(system: ExtendedActorSystem): AddressExtension = new AddressExtension(system)
    /** Get the host address used by the specified system */
    def hostOf(system: ActorSystem): Option[String] = AddressExtension(system).address.host
    /** Get the TCP port used by the specified system */
    def portOf(system: ActorSystem): Option[Int]    = AddressExtension(system).address.port
  }
  /** Extend Akka ActorSystem with address field */
  class AddressExtension(system: ExtendedActorSystem) extends Extension {
    val address: Address = system.provider.getDefaultAddress
  }
}
