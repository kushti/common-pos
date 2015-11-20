package scorex.actors

import akka.actor.Actor
import scorex.cpos.{NotCompleted, PreBlock2, PreBlock1, Block, TypesAndConstants}

import scala.collection.mutable
import scala.util.Random

class Miner extends Actor {

  import TypesAndConstants._

  val pk = Array.fill(32)(Random.nextInt(255).toByte)
  val blockchain: BlockChain = mutable.IndexedSeq[Block]()

  val receipts = mutable.Buffer[NotCompleted]()

  override def receive = {
    case b: Block =>
      blockchain :+ b

    case pb: PreBlock1 =>
      receipts :+ pb


    case pb: PreBlock2 =>
      receipts :+ pb
  }

}

object MinerSpec {

}
