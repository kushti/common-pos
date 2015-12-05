package cpos.actors

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging, ActorRef}
import cpos.model._

import scala.collection.mutable
import scala.util.{Random, Try}

class Miner(environment: ActorRef) extends Actor with ActorLogging {

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

  private var ownWinningTicket: Option[Ticket3] = None

  private def generateTickets(): Unit ={
    val ticket1Block = blockchain(blockchain.size - 3)
    val ticket2Block = blockchain(blockchain.size - 2)
    val ticket3Block = blockchain(blockchain.size - 1)

    val ticket1Candidate = Ticket1(ticket1Block.puz, acc)
    val ticket2Candidate = Ticket2(ticket2Block.puz, acc)
    val ticket3Candidate = Ticket3(ticket3Block.puz, acc)

    if (ticket1Candidate.score > 0) {
      log.info(s"Generated ticket1: $ticket1Candidate")
      ticket1s += ticket1Candidate
      environment ! ticket1Candidate
    }

    if (ticket2Candidate.score > 0) {
      log.info(s"Generated ticket2: $ticket2Candidate")
      ticket2s += ticket2Candidate
      environment ! ticket2Candidate
    }

    if (ticket3Candidate.score > 0) {
      log.info(s"Generated ticket3: $ticket3Candidate")
      ticket3s += ticket3Candidate
      environment ! ticket3Candidate
      ownWinningTicket = Some(ticket3Candidate)
    }
  }

  override def preStart = {
    generateTickets()
  }

  override def receive = {
    case t1: Ticket1 =>
      ticket1s += t1

    case t2: Ticket2 =>
      ticket2s += t2

    case t3: Ticket3 =>
      ticket3s += t3
      if (t3.score > ownWinningTicket.map(_.score).getOrElse(0L)) {
        ownWinningTicket = None
      }

    case b: Block =>
      blockchain :+ b
      generateTickets()

    case TimerUpdate(time) =>
      val lastBlock = blockchain.last
      if (time - lastBlock.time > 20 && ownWinningTicket.isDefined) {
        val t1Puz = blockchain(blockchain.size - 3).puz
        val t2Puz = blockchain(blockchain.size - 2).puz

        val t1Opt = Try(ticket1s.filter(_.puz.sameElements(t1Puz)).maxBy(_.score)).toOption
        val t2Opt = Try(ticket2s.filter(_.puz.sameElements(t2Puz)).maxBy(_.score)).toOption

        (t1Opt, t2Opt) match {
          case (Some(t1), Some(t2)) =>

            val newPuz =
              lastBlock.puz ++
                t1.account.publicKey ++
                t2.account.publicKey ++
                ownWinningTicket.get.account.publicKey

            val newBlock = Block(time, newPuz, t1, t2, ownWinningTicket.get, acc)
            environment ! newBlock

          case _ =>
            log.error("No tickets found")
        }
      }

    case nonsense: Any =>
      log.warning(s"Got strange input: $nonsense")
  }
}


object MinerSpec {

  case class TimerUpdate(time: Long)

}
