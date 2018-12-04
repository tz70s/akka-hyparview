package hyparview.remote

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{Flow, Framing, Keep, Tcp}
import akka.util.{ByteString, Timeout}
import hyparview.remote.WireFormat.Skeleton

/**
 * INTERNAL API.
 *
 * Server logic for handling incoming connection.
 * The core tcp flow need to be handled carefully, and fully tested.
 * Here's the processing flow:
 *
 * When Tcp Connection is coming:
 *
 * 1. Register a KillSwitch for completion or abort.
 * 2. Join two times for two cyclic graph
 *   - framing and serialization phase.
 *   - core logic from IO handler.
 */
private[remote] class TcpIO(config: RemoteConfig)(implicit as: ActorSystem, mat: ActorMaterializer) {

  Tcp().bind(config.hostname, config.port).runForeach { fanIn =>
    // Fork a new child to handle this.
    val io = as.actorOf(IO.props())

    implicit val askTimeout = Timeout(IO.IOAskTimeOut)

    // Core handler for incoming connection.
    // Note that this connection will be kept reused.
    // The handler transform ByteString to Skeleton messages which are handled via IO actor.
    // Associated with fanIn connection, for further control, i.e. close it.
    // TODO: manage this with a more accurate graph -> IOGraph.
    val handler = Flow[ByteString]
      .viaMat(KillSwitches.single)(Keep.right)
      .via(TcpIOCodec.decode)
      .via(deserialize)
      .map((_, fanIn))
      .ask(io)
      .via(serialize)
      .via(TcpIOCodec.encode)

    val killSwitch = fanIn.handleWith(handler)
  }

  private val serialization = SerializationExtension(as)
  private val serializer = serialization.serializerFor(classOf[Skeleton])

  /** Deserialize ByteString to Skeleton case class. */
  // TODO: it would be faster if we do the serialization only once.
  private val deserialize = Flow[ByteString]
    .map(bs => serializer.fromBinary(bs.toArray, Some(classOf[Skeleton])).asInstanceOf[Skeleton])
    .async

  private val serialize = Flow[Skeleton].map(sk => ByteString(serializer.toBinary(sk))).async
}

private[remote] object TcpIO {
  def apply(config: RemoteConfig)(implicit as: ActorSystem, mat: ActorMaterializer): TcpIO = new TcpIO(config)
}

/**
 * INTERNAL API.
 *
 * The Framing Flow of encoding/decoding messages.
 * Use the length field framing via Akka provided.
 * The remain payload will be parsed via protocol buffer serializer.
 */
private[remote] object TcpIOCodec {
  private val MaxFrameLength = Int.MaxValue >> 1 // avoid signed bit.

  val encode = Flow[ByteString].via(Framing.simpleFramingProtocolEncoder(MaxFrameLength))
  val decode = Flow[ByteString].via(Framing.simpleFramingProtocolDecoder(MaxFrameLength))
}
