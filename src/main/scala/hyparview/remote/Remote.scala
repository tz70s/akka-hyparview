package hyparview.remote

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer

/**
 * INTERNAL API.
 *
 * Entry point to construct remote capability.
 * Will contained a Tcp server, multiple incoming and outgoing connections.
 * It will hook up a message passing interface for loosely coupled interaction.
 */
private[remote] class Remote(implicit as: ActorSystem, mat: ActorMaterializer) {

  private val log = Logging(as, this.getClass)

  private val tcpConf = try {
    val conf = as.settings.config
    val hostname = conf.getString("hyparview.remote.hostname")
    val port = conf.getInt("hyparview.remote.port")
    RemoteConfig(hostname, port)
  } catch {
    case t: Throwable =>
      log.error("Get self address failed, do you config this correctly?")
      throw t
  }

  private val tcpIO = TcpIO(tcpConf)
}

/**
 * INTERNAL API.
 */
object Remote {
  def apply()(implicit as: ActorSystem, mat: ActorMaterializer): Remote = new Remote
}

/**
 * INTERNAL API.
 *
 * Configuration loaded from config, for representing binding address as this node.
 */
private[remote] case class RemoteConfig(hostname: String, port: Int)
