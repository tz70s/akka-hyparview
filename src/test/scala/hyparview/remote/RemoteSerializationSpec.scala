package hyparview.remote

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import hyparview.ClusterFormat.HostId
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/** Should carefully place this, to avoid `this` and related fields to be serialized. */
case class FixtureMessage(seq: Int, message: String)

class RemoteSerializationSpec
    extends TestKit(ActorSystem("RemoteSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "Remote Serialization" must {

    "serialize protobuf defined case class from ScalaPb" in {
      val id = HostId("127.0.0.1", 8080)
      val bytes = id.toByteArray
      val revert = HostId.parseFrom(bytes)
      id shouldBe revert
    }

    "serialize protobuf defined case class via serialization extension" in {
      val serialization = SerializationExtension(system)
      val id = HostId("127.0.0.1", 8080)
      val serializer = serialization.findSerializerFor(id)
      val bytes = serializer.toBinary(id)
      val revert = serializer.fromBinary(bytes, Some(classOf[HostId]))
      revert should be(id)
    }
  }
}
