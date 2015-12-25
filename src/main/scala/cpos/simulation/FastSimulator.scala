package cpos.simulation

import java.io._

import cpos.model._
import cpos.util.HashImpl
import org.mapdb.{Atomic, DBMaker, HTreeMap}
import probability_monad.Distribution

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Random, Try}
import scalaxy.loops._

case class BlockchainState(blockchain: HTreeMap[Int, Block],
                           height: Atomic.Integer,
                           accounts: java.util.Set[Account],
                           id: String
                          )

object BlockchainState {

  val file = new File(s"blockchain.db")

  //.mmapFileEnableIfSupported()
  val db = DBMaker.newMemoryDB().closeOnJvmShutdown().checksumEnable().make()

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

  def create(): BlockchainState = {
    val suffix = Random.nextString(30)
    val s = BlockchainState(
      db.createHashMap(s"blockchain-$suffix").makeOrGet[Int, Block](),
      db.getAtomicInteger(s"height-$suffix"),
      db.createHashSet(s"accounts-$suffix").makeOrGet[Account](),
      suffix
    )

    appendBlock(s, new GenesisBlock(1))
    appendBlock(s, new GenesisBlock(2))
    appendBlock(s, new GenesisBlock(3))

    s
  }

  def delete(bs: BlockchainState) = {
    db.delete(s"blockchain-${bs.id}")
    db.delete(s"height-${bs.id}")
    db.delete(s"accounts-${bs.id}")
    db.commit()
  }

}

object FastSimulator extends App {

  val MinersCount = 800

  def generateNormalState() = {

    val normalState = BlockchainState.create()

    val balances = Distribution.exponential(50).sample(MinersCount).map(_ * 100000000).map(_.toInt)

    balances.foreach { b =>
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      normalState.accounts.add(Account(b, pk))
    }

    // println("Big guys: " + balances.filter(_ > 10000000))

    normalState
  }

  def generateAttackerState(normalState: BlockchainState, attackerPercent: Int, accountsNum: Int) = {
    val attackerState = BlockchainState.create()

    val total = normalState.accounts.map(_.balance).sum

    val attackerBalance = ((total * attackerPercent.toLong) / 100).toInt

    //println("Attacker balance: " + attackerBalance)

    val k = accountsNum
    //1,674,545,171
    1 to k foreach { _ =>
      val balance = attackerBalance / k
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      attackerState.accounts.add(Account(balance, pk))
    }
    attackerState
  }

  def genTickets(blockchainState: BlockchainState, accounts: IndexedSeq[Account]):
  (Option[Ticket1], Option[Ticket2], Option[Ticket3]) = {

    val h = BlockchainState.height(blockchainState)

    val p1 = BlockchainState.blockAt(blockchainState, h - 2).seed
    val p2 = BlockchainState.blockAt(blockchainState, h - 1).seed
    val p3 = BlockchainState.blockAt(blockchainState, h).seed

    var best1: Option[Ticket1] = None
    var best2: Option[Ticket2] = None
    var best3: Option[Ticket3] = None

    val filterAccs = blockchainState.blockchain.takeRight(10).values.flatMap { b =>
      Seq(b.ticket1.account, b.ticket2.account, b.ticket3.account)
    }

    for (i <- accounts.indices optimized) {
      val acc = accounts(i)

      if (!filterAccs.contains(acc)) {
        val s1 = Ticket.score(p1, acc, 1)
        val s2 = Ticket.score(p2, acc, 2)
        val s3 = Ticket.score(p3, acc, 3)

        if (s1 > 0 && s1 > best1.map(_.score).getOrElse(0)) best1 = Some(Ticket1(p1, acc))
        if (s2 > 0 && s2 > best2.map(_.score).getOrElse(0)) best2 = Some(Ticket2(p2, acc))
        if (s3 > 0 && s3 > best3.map(_.score).getOrElse(0)) best3 = Some(Ticket3(p3, acc))
      }
    }

    (best1, best2, best3)
  }

  def generateBlocks(blockchainState: BlockchainState, howMany: Int): Unit = {

    val accounts: IndexedSeq[Account] = blockchainState.accounts.toIndexedSeq


    for (i <- 1 until howMany + 1 optimized) {
      val ts = genTickets(blockchainState, accounts)

      val t1 = ts._1.get
      val t2 = ts._2.get
      val t3 = ts._3.get

      val lastBlock = BlockchainState.last(blockchainState)

      val newPuz =
        HashImpl.hash(
          lastBlock.seed ++
            t1.account.publicKey ++
            t2.account.publicKey ++
            t3.account.publicKey)

      val b = Block(lastBlock.height + 1, lastBlock.time + 15, newPuz, t1, t2, t3, t1.account)
      //println("Block generated: " + b)
      BlockchainState.appendBlock(blockchainState, b)

      if (BlockchainState.height(blockchainState) % 10 == 0) System.gc()
    }
  }

  val BlocksToGenerate = 60
  val attackerPercent = 40
  val attackerAccounts = 96

  val as = (1 to 30 map { _ =>
    Try {
      val normalState = generateNormalState()

      generateBlocks(normalState, BlocksToGenerate).ensuring(normalState.blockchain.size() == 3 + BlocksToGenerate)
      val normalScore = normalState.blockchain.map(_._2.score).sum

      val attackerState = generateAttackerState(normalState, attackerPercent, attackerAccounts)
      generateBlocks(attackerState, BlocksToGenerate).ensuring(normalState.blockchain.size() == 3 + BlocksToGenerate)
      val attackerScore = attackerState.blockchain.map(_._2.score).sum


      println("normal:" + normalScore)
      println("attack:" + attackerScore)

      /*attackerState.blockchain.map(_._2).foreach { b =>
        println(b.ticket1)
        println(b.ticket2)
        println(b.ticket3)
      }*/


      BlockchainState.delete(attackerState)
      BlockchainState.delete(normalState)

      attackerScore > normalScore
    }.getOrElse(false)
  }).count(_ == true)

  println("success count: " + as)

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
