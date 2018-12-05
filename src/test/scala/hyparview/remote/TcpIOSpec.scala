package hyparview.remote

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import hyparview.remote.WireFormat.Skeleton
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.google.protobuf
import org.scalatest.concurrent.ScalaFutures

class TcpIOSpec
    extends TestKit(ActorSystem("TcpIOSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  private val serialization = SerializationExtension(system)
  private val serializer = serialization.serializerFor(classOf[Skeleton])

  private def fixtureOf(payload: String) =
    Skeleton(Skeleton.Manifest.HyParViewMessageManifest, protobuf.ByteString.copyFromUtf8(payload))

  "TcpIO Framing" must {
    "correctly frame with length" in {
      implicit val mat = ActorMaterializer()
      val text1 = "Hello, world!"
      val text2 = "Hi, hi, hi!"
      val msg1 = serializer.toBinary(fixtureOf(text1))
      val msg2 = serializer.toBinary(fixtureOf(text2))

      val frameResult = Source(List(msg1, msg2))
        .map(akka.util.ByteString(_))
        .via(TcpFraming.deframe)
        .via(TcpFraming.frame)
        .map(bs => serializer.fromBinary(bs.toArray, Some(classOf[Skeleton])))
        .runWith(Sink.seq)

      whenReady(frameResult) { seq =>
        seq shouldBe Seq(fixtureOf(text1), fixtureOf(text2))
        Skeleton.Manifest.HyParViewMessageManifest shouldBe seq.head
          .asInstanceOf[Skeleton]
          .manifest
      }
    }
  }
}
