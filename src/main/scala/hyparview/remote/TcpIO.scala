package hyparview.remote

import java.net.InetSocketAddress

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Keep, Source, SourceQueueWithComplete, Tcp}
import akka.util.{ByteString, Timeout}
import hyparview.remote.SingleIO.SingleIOProtocol

import scala.util.{Failure, Success}
import akka.pattern.ask

import scala.concurrent.duration._

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
 *   - framing phase.
 *   - core logic from SingleIO handler.
 */
private[remote] class TcpIO(config: RemoteConfig)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private implicit val ec = system.dispatcher

  private val log = Logging(system, classOf[TcpIO])

  private val serverBinding = Tcp().bind(config.hostname, config.port).runForeach { connection =>
    val (singleIO, handler) = tcpHandler(connection.remoteAddress)
    val queue = connection.handleWith(handler)
    initializeSingleIO(singleIO, queue)
  }

  serverBinding.onComplete {
    case Success(Done) =>
      log.info(s"Successfully close out server binding.")
    case Success(result) =>
      log.error(s"Unexpected message from server binding completion: $result")
    case Failure(ex) =>
      log.error(s"Error occurred on server binding, please checkout: ${ex.getMessage}")
  }

  /**
   * Generate an outbound stream for outgoing connection.
   * Similar logic to incoming.
   * @param address The connected target address.
   * @return ActorRef of SingleIO actor.
   */
  def outboundStream(address: InetSocketAddress): ActorRef = {
    // TODO: use restart with backoff strategy.
    val outFlow = Tcp()
      .outgoingConnection(address)
      .recoverWithRetries(3, {
        case ex: Throwable =>
          log.warning(s"Connection failed, retry three times.")
          throw ex
      })
    val (singleIO, handler) = tcpHandler(address)
    val queue = outFlow.joinMat(handler)(Keep.right).run()
    initializeSingleIO(singleIO, queue)
    singleIO
  }

  private def initializeSingleIO(singleIO: ActorRef,
                                 queue: SourceQueueWithComplete[SingleIOProtocol],
                                 retries: Int = 3): Unit =
    (singleIO ? queue)(Timeout(1.second)) recover {
      case ex: Throwable =>
        if (retries <= 0) {
          log.error(s"SingleIO initialization failed - ${ex.getMessage}.")
          system.stop(singleIO)
        } else initializeSingleIO(singleIO, queue, retries - 1)
    }

  private def tcpHandler(
      address: InetSocketAddress
  ): (ActorRef, Flow[ByteString, ByteString, SourceQueueWithComplete[SingleIOProtocol]]) = {
    import SingleIO._

    val killSwitch = KillSwitches.shared(s"KillSwitchOf$address")
    val singleIO = system.actorOf(SingleIO.props(killSwitch))

    // FIXME: How can we avoid this initialization order?
    val outgoingQueue = Source.queue[SingleIOProtocol](16, OverflowStrategy.backpressure)

    // TODO: Use error handling with backoff?
    // The following possibilities of failures:
    // 1. framing -> close connection.
    // 2. actor crash (check with retries) and then close connection.
    val handler = Flow[ByteString]
      .via(killSwitch.flow)
      .via(TcpFraming.frame)
      .map(SingleIOFrame)
      .ask(singleIO)
      .orElseMat(outgoingQueue)(Keep.right)
      .map(_.getOrEmpty())
      .viaMat(TcpFraming.deframe)(Keep.left)

    (singleIO, handler)
  }
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
private[remote] object TcpFraming {
  private val MaxFrameLength = Int.MaxValue >> 1 // avoid signed bit.

  val deframe = Flow[ByteString].via(Framing.simpleFramingProtocolEncoder(MaxFrameLength))
  val frame = Flow[ByteString].via(Framing.simpleFramingProtocolDecoder(MaxFrameLength))
}
