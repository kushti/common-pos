package scorex.actors

import java.security.SecureRandom

import akka.actor.Actor
import scorex.cpos._

import scala.collection.mutable
import scala.util.Random

class Miner extends Actor {
  import CposFunctions._
  import MinerSpec._
  import TypesAndConstants._

  val pk = {
    val bytes = new Array[Byte](PubKeyLength)
    new SecureRandom().nextBytes(bytes)
    bytes
  }
  val blockchain: BlockChain = mutable.IndexedSeq[Block](GenesisBlock)

  val acc = new Account(Random.nextInt(1000000), pk)

  lazy val id = pk.take(2).mkString("")

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
      val r = round(blockchain.last, time)

      val grOpt = r.flatMap(v => checkRight(acc, blockchain.last, v))
      println("id: " + id + "  time: " + time + "  gr: " + grOpt)
      grOpt.foreach(gr => sender() ! gr)
  }
}


object MinerSpec {
  case class TimerUpdate(time:Long)
}
