package hyparview.remote

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Tcp}
import akka.util.ByteString

private[remote] class TcpIO(config: RemoteConfig)(implicit as: ActorSystem, mat: ActorMaterializer) {
  private val fanIns = Tcp().bind(config.hostname, config.port)

}

private[remote] object TcpIO {
  def apply(config: RemoteConfig)(implicit as: ActorSystem, mat: ActorMaterializer): TcpIO = new TcpIO(config)
}

/**
 * INTERNAL API.
 */
@deprecated(since = "0.2")
private[remote] object TcpIOCodec {
  val HyParViewMagic = ByteString("HyParViewMagic")
  val MaxFrameLength = 1024

  /**
   * The Flow of delimiter.
   * Since the protocol should be strictly constructed, we don't allow truncation for the last one.
   * The newline is always required as the end of protocol.
   *
   * Note that this delimiter for protocol is a temporary construction; it is neither correct or efficient.
   * We'll do efficient serialization/deserialization protocol later.
   */
  private val delimiter =
    Flow[ByteString].via(
      Framing.delimiter(HyParViewMagic, maximumFrameLength = MaxFrameLength, allowTruncation = false)
    )
}
