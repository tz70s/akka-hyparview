package hyparview.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import hyparview.HyParView.Identifier

private[network] class Selector extends Actor with ActorLogging {
  import Selector._

  // Adopt a fixed url for discovery: akka.tcp://<actor_system>@<ip>:<port>/user/membership/selector
  // Then other node will use actorSelection for checking out reachability.

  // Note that, this should be distinguished by ActiveView for modularized abstraction.
  private var connections = Map[ActorRef, Identifier]()

  override def receive: Receive = {
    case Connection(id) =>
      // Create child actor -> Connection actor.
      val conn = context.actorOf(Channel.props(Some(id)))

    case Channel.Reachable(id) =>
      connections += (sender() -> id)
      context.parent ! ConnectionSuccess(id)

    case Channel.JoinChannel(src) =>
      // Reactive accept connection.
      val conn = context.actorOf(Channel.props())
      try {
        // Such extraction will be failed in multi-jvm test.
        val id = Identifier(src.path.address.host.get, src.path.address.port.get)
        connections += (conn -> id)
        // Propagate to ack with it.
        conn ! Channel.JoinChannel(src)
        context.parent ! ConnectionSuccess(id)
      } catch {
        case e: Throwable =>
          log.warning(s"Failed to extract incoming connection address, ${e.getMessage}")
          context.stop(conn)
          src ! Channel.JoinChannelFailure
      }

    case Channel.ChannelClose =>
      // Safe, otherwise crash here for debugging.
      val id = connections(sender())
      connections -= sender()
      context.parent ! ConnectionClose(id)

    case Channel.Unreachable(id) =>
      connections.find { case (_, pid) => pid == id }.foreach {
        case (ref, pid) =>
          connections -= ref
          context.parent ! ConnectionFailed(pid)
      }
  }

}

private[hyparview] object Selector {

  case class Connection(id: Identifier)
  case class ConnectionSuccess(id: Identifier)
  case class ConnectionFailed(id: Identifier)
  case class ConnectionClose(id: Identifier)

  val SELECTOR_ACTOR_NAME = "selector"
  def props = Props(new Selector)
}
