package unichat.io.swagger.client.api

import java.io.Closeable

import com.wordnik.swagger.client._

class AsyncClient(config: SwaggerConfig) extends Closeable {
  lazy val locator: ServiceLocator = config.locator
  lazy val name: String = config.name

  private[this] val client = transportClient

  protected def transportClient: TransportClient = new RestClient(config)

  def close() {
    client.close()
  }
}
