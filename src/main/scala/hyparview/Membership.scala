package hyparview

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

/**
 * Membership service for identify nodes.
 * This actor will be implicitly created when using `Cluster(actorSystem)` call.
 * As you may imagine that Akka Cluster did.
 *
 * INTERNAL API.
 */
private[hyparview] class Membership extends Actor with ActorLogging {
  import Membership._

  // TODO: this may have more rich information, instead of actor ref, how about a Subscriber case class?
  private var subscribers = Set[ActorRef]()

  override def receive: Receive = {
    case Subscription(ref) =>
      subscribers += ref

    case UnSubscription(ref) =>
      subscribers -= ref
  }
}

/**
 * INTERNAL API.
 */
private[hyparview] object Membership {
  def props = Props(new Membership)

  case class Subscription(actorRef: ActorRef)
  case class UnSubscription(actorRef: ActorRef)
}
