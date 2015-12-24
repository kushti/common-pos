package cpos.actors

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging, ActorRef}
import cpos.model._

import scala.collection.mutable
import scala.util.Try

class Miner(environment: ActorRef, balance: Int) extends Actor with ActorLogging {

  import MinerSpec._
  import TypesAndConstants._

  val pk = {
    val bytes = new Array[Byte](PubKeyLength)
    new SecureRandom().nextBytes(bytes)
    bytes
  }

  var blockchain: BlockChain = mutable.Buffer[Block](new GenesisBlock(1), new GenesisBlock(2), new GenesisBlock(3))

  val acc = new Account(balance, pk)

  lazy val id = pk.take(3).mkString("")

  val ticket1s = mutable.Buffer[Ticket]()
  val ticket2s = mutable.Buffer[Ticket]()
  val ticket3s = mutable.Buffer[Ticket]()

  private var ownWinningTicket: Option[Ticket3] = None

  private def generateTickets(): Unit = {
    val ticket1Block = blockchain(blockchain.size - 3)
    val ticket2Block = blockchain(blockchain.size - 2)
    val ticket3Block = blockchain.last

    val ticket1Candidate = Ticket1(ticket1Block.seed, acc)
    val ticket2Candidate = Ticket2(ticket2Block.seed, acc)
    val ticket3Candidate = Ticket3(ticket3Block.seed, acc)

    if (ticket1Candidate.score > 0) {
      //  log.info(s"Generated ticket1: $ticket1Candidate")
      ticket1s += ticket1Candidate
      environment ! ticket1Candidate
    }

    if (ticket2Candidate.score > 0) {
      //log.info(s"Generated ticket2: $ticket2Candidate")
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
    log.info(s"balance: " + acc.balance)
    generateTickets()
  }

  override def receive = {
    case t1: Ticket1 =>
      ticket1s += t1

    case t2: Ticket2 =>
      ticket2s += t2

    case t3: Ticket3 =>
      ticket3s += t3
      if (t3.score > ownWinningTicket.map(_.score).getOrElse(0L) && t3.blockPuz.sameElements(blockchain.last.seed)) {
        ownWinningTicket = None
      }

    case b: Block =>
      if (blockchain.last.height == b.height && b.score >= blockchain.last.score) {
        println("Duplicate resolving")
        blockchain = blockchain.dropRight(1)
      }
      blockchain += b
      ticket1s.clear()
      ticket2s.clear()
      ticket3s.clear()
      generateTickets()

    case TimerUpdate(time) =>
      val lastBlock = blockchain.last
      if (time - lastBlock.time > 15 && ownWinningTicket.isDefined) {
        val t1Puz = blockchain(blockchain.size - 3).seed
        val t2Puz = blockchain(blockchain.size - 2).seed

        val t1Opt = Try(ticket1s.filter(_.blockPuz.sameElements(t1Puz)).maxBy(_.score)).toOption
        val t2Opt = Try(ticket2s.filter(_.blockPuz.sameElements(t2Puz)).maxBy(_.score)).toOption

        (t1Opt, t2Opt) match {
          case (Some(t1: Ticket1), Some(t2: Ticket2)) =>

            val newPuz =
              lastBlock.seed ++
                t1.account.publicKey ++
                t2.account.publicKey ++
                ownWinningTicket.get.account.publicKey

            val newBlock = Block(lastBlock.height + 1, time, newPuz, t1, t2, ownWinningTicket.get, acc)
            environment ! newBlock
            ownWinningTicket = None

          case _ =>
            log.error("No tickets found")
        }
      }

    case AnalyzeChain(balances, total) =>
      val chain = blockchain.drop(3)
      println("Blocks generated: " + chain.size)
      println("balances:" + balances)
      val allTickets = chain.size * 3
      val tickets: Seq[Ticket] = chain.flatMap(b => Seq(b.ticket1, b.ticket2, b.ticket3))
      //out balance -> number of tickets
      val stats = tickets.map(t => t.account -> t.score).groupBy(_._1).map { case (a, s) =>
        100 * a.balance.toDouble / total -> 100 * s.map(_._2).size.toDouble / allTickets
      }.toSeq.sortBy(_._1)
      println("Accounts generated tickets: " + stats.size)
      println(stats.mkString("\n"))
      context.system.terminate()

    case nonsense: Any =>
      log.warning(s"Got strange input: $nonsense")
  }
}


object MinerSpec {

  case class AnalyzeChain(balances: Seq[Int], totalBalance: Long)

  case class TimerUpdate(time: Long)

}
