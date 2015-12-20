package cpos.simulation

import cpos.model.TypesAndConstants._
import cpos.model._
import probability_monad.Distribution

import scala.collection.mutable
import scala.language.postfixOps
import scala.util.Random
import scalaxy.loops._

object FastSimulator extends App {

  val BlocksToGenerate = 5000

  val MinersCount = 800

  val balances = Distribution.exponential(50).sample(MinersCount).map(_ * 100000000).map(_.toInt)

  val accs = balances.map { b =>
    val pk = new Array[Byte](32)
    Random.nextBytes(pk)
    Account(b, pk)
  }.toIndexedSeq

  val blockchain: BlockChain = mutable.Buffer[Block](new GenesisBlock(1), new GenesisBlock(2), new GenesisBlock(3))

  def genTickets(blockchain: BlockChain):
  (Option[Ticket1], Option[Ticket2], Option[Ticket3]) = {

    val p1 = blockchain(blockchain.size - 3).puz
    val p2 = blockchain(blockchain.size - 2).puz
    val p3 = blockchain(blockchain.size - 1).puz

    var best1: Option[Ticket1] = None
    var best2: Option[Ticket2] = None
    var best3: Option[Ticket3] = None

    for(i <- accs.indices optimized){
      val acc = accs(i)

      val s1 = Ticket.score(p1, acc, 1)
      val s2 = Ticket.score(p2, acc, 2)
      val s3 = Ticket.score(p3, acc, 3)

      if (s1 > 0 && s1 > best1.map(_.score).getOrElse(0)) best1 = Some(Ticket1(p1, acc))
      if (s2 > 0 && s2 > best2.map(_.score).getOrElse(0)) best2 = Some(Ticket2(p2, acc))
      if (s3 > 0 && s3 > best3.map(_.score).getOrElse(0)) best3 = Some(Ticket3(p3, acc))
    }

    (best1, best2, best3)
  }

  println("Big guys: " + balances.filter(_ > 10000000))

  for (i <- 1 until BlocksToGenerate optimized) {
    val ts = genTickets(blockchain)

    val t1 = ts._1.get
    val t2 = ts._2.get
    val t3 = ts._3.get

    val newPuz =
      blockchain.last.puz ++
        t1.account.publicKey ++
        t2.account.publicKey ++
        t3.account.publicKey

    val b = Block(blockchain.size + 1, blockchain.last.time + 15, newPuz, t1, t2, t3, t1.account)
    println("Block generated: " + b)
    blockchain += b

    if (blockchain.size % 10 == 0) System.gc()
  }

  val chain = blockchain.drop(3)
  val total = balances.map(_.toLong).sum

  println("Blocks generated: " + chain.size)
  println("balances:" + balances)
  println("Big guys: " + balances.filter(_ > 10000000))
  val allTickets = chain.size * 3
  val tickets: Seq[Ticket] = chain.flatMap(b => Seq(b.ticket1, b.ticket2, b.ticket3))
  //out balance -> number of tickets
  val stats = tickets.map(t => t.account -> t.score).groupBy(_._1).map { case (a, s) =>
    100 * a.balance.toDouble / total -> 100 * s.map(_._2).size.toDouble / allTickets
  }.toSeq.sortBy(_._1)
  println("Accounts generated tickets: " + stats.size)
  println(stats.mkString("\n"))
}
