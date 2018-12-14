package hyparview.remote

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.{ByteString, Timeout}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

import akka.pattern.pipe

/**
 * INTERNAL API.
 *
 * Actor for handling incoming and outgoing message, and the lifecycle of stream.
 */
private[remote] class SingleIO(private val killSwitch: SharedKillSwitch)
    extends FSM[SingleIO.State, SingleIO.Data]
    with ActorLogging {

  import SingleIO._

  private implicit val ec = context.dispatcher

  private var outgoingQueue: SourceQueueWithComplete[SingleIOProtocol] = _

  startWith(Idle, Cache.empty)

  when(Idle, stateTimeout = 3.seconds) {
    case Event(StateTimeout, _) =>
      throw SingleIOIdleTimeoutException(
        "Idle timeout, the required data (kill switch and source queue) doesn't reach."
      )

    case Event(queue: SourceQueueWithComplete[SingleIOProtocol] @unchecked, _) =>
      // The Source Queue will erasure the internal message type.
      outgoingQueue = queue
      goto(Active) using Cache.empty

    case Event(frame: SingleIORead, cache: Cache) =>
      cache.data += ((sender(), frame))
      stay using cache
  }

  onTransition {
    case Idle -> Active =>
      stateData match {
        case cache: Cache =>
          log.debug(s"Drained out transient cache data, handle reply back before state transition.")
          for (entry <- cache.data; (ref: ActorRef, frame: SingleIORead) = entry) {
            readHandling(frame)
            ref ! ByteString.empty
          }
        case _ =>
      }
  }

  private def readHandling(frame: SingleIORead): Unit =
    log.debug(s"Cope with new frame : $frame")

  when(Active) {
    case Event(read: SingleIORead, _) =>
      readHandling(read)
      sender() ! ByteString.empty
      stay

    case Event(write: SingleIOWrite, _) =>
      outgoingQueue.offer(write).map(_ => SingleIOWriteAck).pipeTo(sender()) // currently, ignore the future value.
      stay
  }

  whenUnhandled {
    case Event(evt, data) =>
      log.warning(s"Received unhandled event $evt in state $stateName/$data.")
      stay
  }

  initialize()

  override def postStop(): Unit = killSwitch.shutdown() // TODO: what about failure shutdown?
}

private[remote] object SingleIO {
  implicit val InboundPushTimeout = Timeout(3.seconds)
  def props(killSwitch: SharedKillSwitch) = Props(new SingleIO(killSwitch))

  sealed trait State
  case object Idle extends State
  case object Active extends State

  sealed trait Data
  case class Cache(data: ArrayBuffer[(ActorRef, SingleIORead)]) extends Data
  object Cache {
    def empty = Cache(ArrayBuffer.empty)
  }

  sealed trait SingleIOProtocol {
    def getOrEmpty: ByteString = this match {
      case SingleIORead(data) => data
      case SingleIOWrite(data) => data
      case _ => ByteString.empty
    }
  }
  case object SingleIOReadAck extends SingleIOProtocol
  case class SingleIORead(data: ByteString) extends SingleIOProtocol
  case class SingleIOWrite(data: ByteString) extends SingleIOProtocol
  case object SingleIOWriteAck extends SingleIOProtocol

  sealed trait SingleIOControlProtocol
  case object SingleIOIsActive extends SingleIOControlProtocol

  case class SingleIOIdleTimeoutException(message: String) extends RuntimeException(message)
}
