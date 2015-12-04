package scorex.actors

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging}
import scorex.cpos._

import scala.collection.mutable
import scala.util.Random

class Miner extends Actor with ActorLogging {
  import MinerSpec._
  import TypesAndConstants._

  val pk = {
    val bytes = new Array[Byte](PubKeyLength)
    new SecureRandom().nextBytes(bytes)
    bytes
  }

  val blockchain: BlockChain = mutable.IndexedSeq[Block](GenesisBlock, GenesisBlock, GenesisBlock)

  val acc = new Account(Random.nextInt(1000000), pk)

  lazy val id = pk.take(3).mkString("")

  val ticket1s = mutable.Buffer[Ticket]()
  val ticket2s = mutable.Buffer[Ticket]()
  val ticket3s = mutable.Buffer[Ticket]()

  val ownReceipts = mutable.Buffer[Ticket]()

  override def receive = {
    case t1: Ticket1 =>
      ticket1s += t1

    case t2: Ticket2 =>
      ticket2s += t2

    case b: Block =>
      blockchain :+ b


    case TimerUpdate(time) =>

  }
}


object MinerSpec {

  case class TimerUpdate(time: Long)

}
