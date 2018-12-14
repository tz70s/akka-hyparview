package hyparview.remote

import java.net.InetAddress

import akka.NotUsed
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.Tcp.{IncomingConnection, OutgoingConnection}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

private[remote] case class HybridFlow private (sink: Sink[ByteString, NotUsed],
                                               source: Source[ByteString, NotUsed],
                                               private val killSwitch: SharedKillSwitch) {
  def close(): Unit = killSwitch.shutdown()
  def closeErr(reason: Throwable): Unit = killSwitch.abort(reason)
}

private[remote] case class NearHybridFlow(var _sink: Option[Sink[ByteString, Future[OutgoingConnection]]],
                                          var _source: Option[Source[ByteString, NotUsed]],
                                          var _killSwitch: Option[SharedKillSwitch])

object HybridFlow {

  /**
   * Proactively generate a HybridFlow to a remote peer.
   * If remote peer reject this request or any error occurred then throws an exception.
   */
  def to(remote: InetAddress): Unit = {}

}
