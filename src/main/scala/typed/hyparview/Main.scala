package typed.hyparview

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main {

  import HyParViewCluster._

  sealed trait SystemStartingProtocol
  final case object OnSuccess extends SystemStartingProtocol
  final case class OnFailure(throwable: Throwable) extends SystemStartingProtocol

  val main: Behavior[SystemStartingProtocol] = Behaviors.setup { context =>
    val cluster = context.spawn(HyParViewCluster.behavior, "cluster")
    implicit val subscribeTimeout = Timeout(1.second)

    context.ask(cluster)(Subscribe(context.system.name, _: ActorRef[ClusterEvent])) {
      case Success(SubAck) => OnSuccess
      case Success(_) => OnFailure(ClusterSubscriptionError("Unexpected response message after subscription."))
      case Failure(throwable) => OnFailure(throwable)
    }

    Behaviors.receiveMessage[SystemStartingProtocol] {
      case OnSuccess =>
        context.log.info(s"Successfully start cluster system.")
        Behavior.same

      case OnFailure(throwable) =>
        context.log.error(s"Starting cluster system failed, close system. ${throwable.getMessage}")
        Behavior.stopped
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(main, "HyParViewTestActorSystem")
  }
}
