package cpos.simulation

import java.io._

import cpos.model._
import org.mapdb.{Atomic, DBMaker, HTreeMap}
import probability_monad.Distribution

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Random
import scalaxy.loops._

case class BlockchainState(blockchain: HTreeMap[Int, Block],
                           height: Atomic.Integer,
                           accounts: java.util.Set[Account])

object FastSimulator extends App {

  val MinersCount = 800

  val file = new File(s"blockchain.db")

  val db = DBMaker.newFileDB(file).mmapFileEnableIfSupported().closeOnJvmShutdown().checksumEnable().make()

  val normalState = BlockchainState(
    db.createHashMap(s"blockchain-$MinersCount-normal").makeOrGet[Int, Block](),
    db.getAtomicInteger(s"height-$MinersCount-normal"),
    db.createHashSet(s"accounts-$MinersCount-normal").makeOrGet[Account]()
  )

  val attackerState = BlockchainState(
    db.createHashMap(s"blockchain-$MinersCount-miners-attacker").makeOrGet[Int, Block](),
    db.getAtomicInteger(s"height-$MinersCount-attacker"),
    db.createHashSet(s"accounts-$MinersCount-attacker").makeOrGet[Account]()
  )

  def appendBlock(state: BlockchainState, block: Block): Unit = {
    val h = block.height
    state.blockchain.put(h, block)
    state.height.set(h)
    db.commit()
  }

  def blockAt(state: BlockchainState, h: Int): Block = state.blockchain.get(h)

  def last(state: BlockchainState) = blockAt(state, state.height.get())

  def height(state: BlockchainState): Int = state.height.get()

  def empty(state: BlockchainState): Boolean = height(state) == 0

  if (empty(normalState)) {
    appendBlock(normalState, new GenesisBlock(1))
    appendBlock(normalState, new GenesisBlock(2))
    appendBlock(normalState, new GenesisBlock(3))

    val balances = Distribution.exponential(50).sample(MinersCount).map(_ * 100000000).map(_.toInt)

    balances.foreach { b =>
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      normalState.accounts.add(Account(b, pk))
    }

    println("Big guys: " + balances.filter(_ > 10000000))
  }

  if (empty(attackerState)) {
    appendBlock(attackerState, new GenesisBlock(1))
    appendBlock(attackerState, new GenesisBlock(2))
    appendBlock(attackerState, new GenesisBlock(3))

    //1,674,545,171
    1 to 500 foreach{_ =>
      val balance = 1000000
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      attackerState.accounts.add(Account(balance, pk))
    }
  }

  def genTickets(blockchainState: BlockchainState, accounts: IndexedSeq[Account]):
  (Option[Ticket1], Option[Ticket2], Option[Ticket3]) = {

    val h = height(blockchainState)

    val p1 = blockAt(blockchainState, h - 2).puz
    val p2 = blockAt(blockchainState, h - 1).puz
    val p3 = blockAt(blockchainState, h).puz

    var best1: Option[Ticket1] = None
    var best2: Option[Ticket2] = None
    var best3: Option[Ticket3] = None

    for (i <- accounts.indices optimized) {
      val acc = accounts(i)

      val s1 = Ticket.score(p1, acc, 1)
      val s2 = Ticket.score(p2, acc, 2)
      val s3 = Ticket.score(p3, acc, 3)

      if (s1 > 0 && s1 > best1.map(_.score).getOrElse(0)) best1 = Some(Ticket1(p1, acc))
      if (s2 > 0 && s2 > best2.map(_.score).getOrElse(0)) best2 = Some(Ticket2(p2, acc))
      if (s3 > 0 && s3 > best3.map(_.score).getOrElse(0)) best3 = Some(Ticket3(p3, acc))
    }

    (best1, best2, best3)
  }

  def generateBlocks(blockchainState: BlockchainState, howMany: Int): Unit = {

    val accounts: IndexedSeq[Account] = blockchainState.accounts.toIndexedSeq

    println("total stake: "+accounts.map(_.balance.toLong).sum) // 1,674,545,171

    for (i <- 1 until BlocksToGenerate optimized) {
      val ts = genTickets(blockchainState, accounts)

      val t1 = ts._1.get
      val t2 = ts._2.get
      val t3 = ts._3.get

      val lastBlock = last(blockchainState)

      val newPuz =
        lastBlock.puz ++
          t1.account.publicKey ++
          t2.account.publicKey ++
          t3.account.publicKey

      val b = Block(lastBlock.height + 1, lastBlock.time + 15, newPuz, t1, t2, t3, t1.account)
      println("Block generated: " + b)
      appendBlock(blockchainState, b)

      if (height(blockchainState) % 10 == 0) System.gc()
    }
  }


  val BlocksToGenerate = 100
  generateBlocks(normalState, 100)
  generateBlocks(attackerState, 100)

  println("normal:" + normalState.blockchain.map(_._2.score).sum)
  println("attack:" + attackerState.blockchain.map(_._2.score).sum)

  /*

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

  */
}
