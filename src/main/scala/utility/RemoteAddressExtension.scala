package utility

import akka.actor.{ActorSystem, Address, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

class RemoteAddressExtension(system: ExtendedActorSystem) extends Extension {
  def address: Address = system.provider.getDefaultAddress
}

object RemoteAddressExtension extends ExtensionId[RemoteAddressExtension]
  with ExtensionIdProvider {
  override def lookup: RemoteAddressExtension.type = RemoteAddressExtension

  override def createExtension(system: ExtendedActorSystem) = new RemoteAddressExtension(system)

  override def get(system: ActorSystem): RemoteAddressExtension = super.get(system)
}