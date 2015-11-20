package scorex.actors

import akka.actor.Actor
import scorex.cpos._

import scala.collection.mutable
import scala.util.Random

class Miner extends Actor {
  import MinerSpec._
  import TypesAndConstants._

  val pk = Array.fill(PubKeyLength)(Random.nextInt(Byte.MaxValue).toByte)
  val blockchain: BlockChain = mutable.IndexedSeq[Block](GenesisBlock)

  val receipts = mutable.Buffer[NotCompleted]()
  val ownReceipts = mutable.Buffer[NotCompleted]()

  override def receive = {
    case b: Block =>
      blockchain :+ b
      receipts.clear()

    case pb: PreBlock1 =>
      receipts :+ pb

    case pb: PreBlock2 =>
      receipts :+ pb

    case TimerUpdate(time) =>

  }
}


object MinerSpec {
  case class TimerUpdate(time:Long)
}
