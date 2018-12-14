package typed.hyparview

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object HyParViewCluster {

  sealed trait ClusterEvent
  sealed trait ClusterControlEvent extends ClusterEvent

  case class Subscribe(host: String, replyTo: ActorRef[ClusterEvent]) extends ClusterControlEvent
  case object SubAck extends ClusterControlEvent

  val behavior: Behavior[ClusterEvent] = handShake()

  private def handShake(table: Map[String, ActorRef[ClusterEvent]] = Map.empty): Behavior[ClusterEvent] =
    Behaviors.receivePartial[ClusterEvent] {
      case (context, Subscribe(host, replyTo)) =>
        context.log.debug(s"HyParViewCluster behavior accepts for $host subscription.")
        replyTo ! SubAck
        handShake(table + (host -> replyTo))
    }
}

case class ClusterSubscriptionError(message: String, cause: Throwable = null) extends Exception(message, cause)
