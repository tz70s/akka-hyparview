package hyparview.remote

import akka.actor.ActorSystem
import akka.event.Logging
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer

import scala.util.Try

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

  // Serialization.
  private val serialization = SerializationExtension(as)
  private def serialize(o: AnyRef): Try[Array[Byte]] =
    // Use the default Java Serializer.
    serialization.serialize(o)

  private val tcpIO = TcpIO(tcpConf)
}

object Remote {
  def apply()(implicit as: ActorSystem, mat: ActorMaterializer): Remote = new Remote
}

/** Load this from config. */
private[remote] case class RemoteConfig(hostname: String, port: Int)
