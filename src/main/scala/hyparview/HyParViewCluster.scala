package hyparview

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import hyparview.HyParView.Identifier

private[hyparview] class HyParViewClusterImpl(system: ExtendedActorSystem) extends Extension {
  import Membership._

  // Start a membership service (actor).
  // Parse contact node from config.

  // TODO: extend contact node to multiple.
  private val contactNode = {
    val config = system.settings.config
    val hostname = config.getString("hyparview.contact-node.hostname")
    val port = config.getInt("hyparview.contact-node.port")
    Identifier(hostname, port)
  }

  private val membership = system.actorOf(Membership.props(contactNode), MembershipActorName)

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

object HyParViewCluster extends ExtensionId[HyParViewClusterImpl] with ExtensionIdProvider {

  override def lookup(): ExtensionId[_ <: Extension] = HyParViewCluster

  override def createExtension(system: ExtendedActorSystem): HyParViewClusterImpl = new HyParViewClusterImpl(system)

  /**
   * Java API: retrieve the HyParViewCluster extension from system.
   */
  override def get(system: ActorSystem): HyParViewClusterImpl = super.get(system)
}

object HyParViewClusterEvent {
  sealed trait MemberEvent
  case class MemberUp(id: Identifier) extends MemberEvent
  case class MemberRemoved(id: Identifier) extends MemberEvent
}
