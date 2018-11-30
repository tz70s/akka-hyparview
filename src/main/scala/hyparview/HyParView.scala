package hyparview

import java.net.Inet4Address

object HyParView {
  // Define set of communication primitives.

  /** Identifier for membership. */
  case class Identifier(ip: Inet4Address, port: Long)

  case class TimeToLive(private val number: Int) {

    /** Decrement a TTL, and generate a new one. */
    def dec: TimeToLive =
      if (expired) {
        throw new IllegalArgumentException(
          "Required TTL number is greater than 0, this check should be ensure in the procedure."
        )
      } else {
        TimeToLive(number - 1)
      }

    /** Check if TTL is expired or not. */
    def expired: Boolean = number == 0
  }

  sealed trait HyParViewMessage

  // Or maybe we'll switch this to ActorRef?
  case class Join(sender: Identifier) extends HyParViewMessage

  case class ForwardJoin(sender: Identifier, newNode: Identifier, timeToLive: TimeToLive) extends HyParViewMessage

  /** Denote that prior is true when priority is high, and otherwise. */
  case class Neighbor(sender: Identifier, prior: Boolean) extends HyParViewMessage

  case class Shuffle() extends HyParViewMessage

  case class ShuffleReply() extends HyParViewMessage

  case class Disconnect() extends HyParViewMessage
}
