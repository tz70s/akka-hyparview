package hyparview.remote

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy, SharedKillSwitch}
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import hyparview.remote.SingleIO.{SingleIOFrame, SingleIOProtocol}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SingleIOSpec
    extends TestKit(ActorSystem("SingleIOSpec"))
    with WordSpecLike
    with Matchers
    with ImplicitSender
    with BeforeAndAfterAll {

  @volatile implicit var mat: ActorMaterializer = _

  override def beforeAll(): Unit = mat = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  def dummySourceQueue =
    Source.queue[SingleIOProtocol](16, OverflowStrategy.backpressure).toMat(Sink.ignore)(Keep.left).run()

  "The SingleIO Actor" must {

    "retained messages during initialization" in {

      val singleIO = system.actorOf(SingleIO.props(KillSwitches.shared("SingleIOSpecKillSwitch")))

      val queue = dummySourceQueue

      singleIO ! SingleIOFrame(ByteString("Hello world!"))
      singleIO ! SingleIOFrame(ByteString("Hello world!"))
      singleIO ! queue
      expectMsg(ByteString.empty)
      expectMsg(ByteString.empty)
    }
  }
}
