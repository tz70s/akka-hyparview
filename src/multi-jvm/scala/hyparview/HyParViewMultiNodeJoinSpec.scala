package hyparview

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.{Config, ConfigFactory}
import hyparview.HyParView.Identifier
import hyparview.HyParViewClusterEvent.MemberUp

object HyParViewMultiNodeJoinConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")

  def shareConfig: Config = ConfigFactory.parseString(s"""
       |akka {
       |  loglevel = INFO
       |
       |  actor.provider = "remote"
       |
       |  remote {
       |    enabled-transports = ["akka.remote.netty.tcp"]
       |    netty.tcp.hostname = "127.0.0.1"
       |  }
       |}
       |
       |hyparview {
       |  contact-node {
       |    hostname = "127.0.0.1"
       |    port = 8080
       |  }
       |}
     """.stripMargin)

  commonConfig(shareConfig)

  nodeConfig(node1)(ConfigFactory.parseString(s"""
       |akka.remote.netty.tcp.port = 8080
       |""".stripMargin))

  nodeConfig(node2)(ConfigFactory.parseString(s"""
       |akka.remote.netty.tcp.port = 8181
       |""".stripMargin))
}

class HyParViewMultiNodeJoinSpec
    extends MultiNodeSpec(HyParViewMultiNodeJoinConfig)
    with STMultiNodeSpec
    with ImplicitSender {
  import HyParViewMultiNodeJoinConfig._

  override def initialParticipants: Int = roles.size

  "A Dual Node Join" must {

    "wait for nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send join message to contact node" in {
      runOn(node1) {
        val cluster = HyParViewCluster(system)
        cluster.subscribe(self)
        enterBarrier("node1-join")
        enterBarrier("node2-join")
        expectMsg(MemberUp(Identifier("127.0.0.1", 8181)))
      }

      runOn(node2) {
        enterBarrier("node1-join")
        val cluster = HyParViewCluster(system)
        cluster.subscribe(self)
        expectMsg(MemberUp(Identifier("127.0.0.1", 8080)))
        enterBarrier("node2-join")
      }
    }
  }
}

// class HyParViewMultiNodeJoinSpecMultiJvmNode1 extends HyParViewMultiNodeJoinSpec
// class HyParViewMultiNodeJoinSpecMultiJvmNode2 extends HyParViewMultiNodeJoinSpec
