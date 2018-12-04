package hyparview

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import hyparview.HyParView.Identifier
import hyparview.HyParViewClusterEvent.MemberUp
import hyparview.network.Selector
import hyparview.network.Selector.{Connection, ConnectionClose, ConnectionFailed, ConnectionSuccess}

/**
 * INTERNAL API.
 *
 * Membership service for identify nodes.
 * This actor will be implicitly created when using `Cluster(actorSystem)` call.
 * As you may imagine that Akka Cluster did.
 *
 * In Akka Remote API, there's no explicit connection close function for safety,
 * but it's required for managing ActiveView connections or even PassiveView connection cache.
 *
 * One possible implementation is:
 * We should try some way to make the connection state transition from `Active` to `Gated` state,
 * and automatically garbage collected via Akka's internal `akka.remote.retry-gate-closed-for` to `Idle` state.
 *
 * However, a notable question is that, the first touch of Membership actor will keep connection or not?
 *
 * From now on, I've create a death-watch for SIMULATE persistent-connection.
 * This should be factored out once I've more time.
 */
private[hyparview] class Membership(contactNode: Identifier) extends Actor with ActorLogging {

  import Membership._

  private val selector = context.actorOf(Selector.props, Selector.SelectorActorName)

  private val selfId = try {
    val config = context.system.settings.config
    val hostname = config.getString("akka.remote.netty.tcp.hostname")
    val port = config.getInt("akka.remote.netty.tcp.port")
    Identifier(hostname, port)
  } catch {
    case e: Throwable =>
      throw new IllegalArgumentException(
        s"Failed to extract self address, is the configuration set correctly? ${e.getMessage}"
      )
  }

  override def preStart(): Unit =
    if (selfId != contactNode) selector ! Connection(contactNode)

  private var subscribers = Set[ActorRef]()

  override def receive: Receive = {
    case ConnectionSuccess(id) =>
      log.info(s"Connection success to $id")
      subscribers.foreach { ref =>
        ref ! MemberUp(id)
      }

    case ConnectionFailed(id) =>
      log.warning(s"Connection failed to $id")

    case ConnectionClose(id) =>
      log.info(s"Connection close from remote $id")

    case Subscription(ref) =>
      // TODO: The first subscription should propagate the current partial view for it.
      subscribers += ref

    case UnSubscription(ref) =>
      subscribers -= ref
  }
}

/**
 * INTERNAL API.
 */
private[hyparview] object Membership {
  val MembershipActorName = "membership"
  def props(contactNode: Identifier) = Props(new Membership(contactNode))

  case class Subscription(actorRef: ActorRef)
  case class UnSubscription(actorRef: ActorRef)
}
