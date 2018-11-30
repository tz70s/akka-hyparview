package hyparview

import akka.actor.{ActorRef, ActorSystem}

class Cluster private (actorSystem: ActorSystem) {
  import Membership._

  // Start a membership service (actor).
  // This is a fixed url for discovery: akka.tcp://<actor_system>@<ip>:<port>/user/membership
  // Then other node will use actorSelection for checking out reachability.
  private val membership = actorSystem.actorOf(Membership.props, "membership")

  /**
   * Subscribe set of MemberEvent for membership service.
   * The difference to Akka, since HyParView has only two types of MemberEvents (up and down).
   * Hence, there's no really needed for distinguished event type for subscription.
   *
   * @param actorRef The actor reference of the actor who subscribed for.
   */
  def subscribe(actorRef: ActorRef): Unit =
    // Register subscription actor to membership actor.
    membership ! Subscription(actorRef)

  def unsubscribe(actorRef: ActorRef): Unit =
    membership ! UnSubscription(actorRef)
}

object Cluster {
  def apply(actorSystem: ActorSystem): Cluster = new Cluster(actorSystem)
}

object ClusterEvent {
  sealed trait MemberEvent
  case class MemberUp() extends MemberEvent
  case class MemberRemoved() extends MemberEvent
}
