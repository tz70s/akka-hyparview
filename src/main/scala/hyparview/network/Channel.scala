package hyparview.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated, Timers}
import hyparview.HyParView.Identifier
import hyparview.Membership

import scala.concurrent.duration._

private[network] class Channel(remoteId: Option[Identifier]) extends Actor with ActorLogging with Timers {

  import Channel._
  import Selector._

  override def preStart(): Unit = remoteId match {
    case Some(id) =>
      // Address the connection node, note that we assume all actor system holds the same name.
      val selectorPath =
        s"akka.tcp://${context.system.name}@${id.hostname}:${id.port}/user/${Membership.MEMBERSHIP_ACTOR_NAME}/$SELECTOR_ACTOR_NAME"

      val selector = context.actorSelection(selectorPath)
      selector ! JoinChannel(self)
      // By default, 3 second timeout for testing reachability.
      timers.startSingleTimer(ChannelTimer, ChannelTimerOut(id), 3.seconds)

    case None =>
    // Reactive creation, no selector involved.
  }

  override def receive: Receive = {
    case JoinChannel(ref) =>
      context.watch(ref)
      ref ! JoinChannelAck

    case JoinChannelAck =>
      context.watch(sender())
      timers.cancel(ChannelTimer)
      // This should be safe, otherwise, let it crashed.
      context.parent ! Reachable(remoteId.get)

    case ChannelTimerOut(id) =>
      context.parent ! Unreachable(id)

    case JoinChannelFailure =>
      // directly throws exception.
      throw new IllegalArgumentException("The remote-side was failed to extract actor address from here.")

    case Terminated(_) =>
      // Since we have 1-1 connection (watch), no need to ensure the peer.
      // Tell parent to close connection.
      context.parent ! ChannelClose
  }
}

private[network] object Channel {

  def props(remoteId: Option[Identifier] = None) = Props(new Channel(remoteId))

  // Channel linking timer primitives.
  case object ChannelTimer
  case class ChannelTimerOut(id: Identifier)

  case class JoinChannel(ref: ActorRef)
  case object JoinChannelAck
  case object JoinChannelFailure
  case object ChannelClose

  case class Reachable(id: Identifier)
  case class Unreachable(id: Identifier)
}
