package scorex.actors

import akka.actor.Actor
import scorex.cpos.{Block, NotCompleted, PreBlock1, PreBlock2, TypesAndConstants}

import scala.collection.mutable
import scala.util.Random

class Miner extends Actor {

  import TypesAndConstants._

  val pk = Array.fill(PubKeyLength)(Random.nextInt(Byte.MaxValue).toByte)
  val blockchain: BlockChain = mutable.IndexedSeq[Block]()

  val receipts = mutable.Buffer[NotCompleted]()

  override def receive = {
    case b: Block =>
      blockchain :+ b
      receipts.clear()

    case pb: PreBlock1 =>
      receipts :+ pb

    case pb: PreBlock2 =>
      receipts :+ pb
  }
}


object MinerSpec {

}
