package hyparview.remote

import java.net.InetSocketAddress

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender
import akka.util.ByteString
import hyparview.STMultiNodeSpec
import hyparview.remote.SingleIO.{SingleIOWrite, SingleIOWriteAck}

object TcpIOMultiNodeConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}

class TcpIOMultiNodeSpec extends MultiNodeSpec(TcpIOMultiNodeConfig) with STMultiNodeSpec with ImplicitSender {

  import TcpIOMultiNodeConfig._

  override def initialParticipants: Int = roles.size

  "A Dual Node with Tcp Connection" must {

    "wait for nodes to enter barrier" in {
      enterBarrier("start-up-spec")
    }

    "spawn tcp service on each node" in {
      runOn(node1) {
        implicit val mat = ActorMaterializer()
        val tcpIO = TcpIO(RemoteConfig("127.0.0.1", 8123))
        enterBarrier("node-start-tcp-service")
        enterBarrier("node2-outbound-connection")
        tcpIO.nrOfConnection shouldBe 1
        enterBarrier("node1-close")
      }

      runOn(node2) {
        implicit val mat = ActorMaterializer()
        val tcpIO = TcpIO(RemoteConfig("127.0.0.1", 8125))
        enterBarrier("node-start-tcp-service")
        val ref = tcpIO.outboundStream(new InetSocketAddress("127.0.0.1", 8123))
        enterBarrier("node2-outbound-connection")
        ref ! SingleIOWrite(ByteString("Hello, World"))
        expectMsg(SingleIOWriteAck)
        enterBarrier("node1-close")
      }
    }
  }
}

class TcpIOMultiNodeSpecMultiJvmNode1 extends TcpIOMultiNodeSpec
class TcpIOMultiNodeSpecMultiJvmNode2 extends TcpIOMultiNodeSpec
