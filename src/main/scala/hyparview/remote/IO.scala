package hyparview.remote

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.GraphDSL
import akka.stream.UniqueKillSwitch
import hyparview.remote.WireFormat.Skeleton

import scala.concurrent.duration._

/**
 * INTERNAL API.
 *
 * Actor for handling incoming and outgoing message.
 */
private[remote] class IO extends Actor with ActorLogging {

  private var killSwitch: UniqueKillSwitch = _

  private def close(throwable: Option[Throwable] = None) = throwable match {
    case None =>
      log.debug(s"Shutdown connection to : ")
      killSwitch.shutdown()
      context.stop(self)
    case Some(ex) =>
      log.error(s"Failure ${ex.getMessage} occurred, close connection.")
      killSwitch.abort(ex)
      context.stop(self)
  }

  override def receive: Receive = {
    case Skeleton(manifest, payload) =>
    case ks: UniqueKillSwitch =>
      killSwitch = ks
  }
}

private[remote] object IO {
  val IOAskTimeOut = 3.seconds
  def props() = Props(new IO)
}

private[remote] object IOGraph {

  def apply[In, Out, Mat]() =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

    }
}
