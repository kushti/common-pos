package cpos.simulation

import cpos.model.TypesAndConstants._
import cpos.model._
import cpos.util.HashImpl
import probability_monad.Distribution

import scala.language.postfixOps
import scala.util.Random


case class NxtBlock(height: Int,
                    time: Time,
                    generationSignature: Array[Byte],
                    baseTarget: Long,
                    generator: Account)


object GenesisBlock extends NxtBlock(1, 0, Array.fill(32)(Random.nextInt(100).toByte), 153722867, GenesisCreator)

case class NxtBlockchainState(blockchain: Map[Int, NxtBlock],
                              height: Int,
                              accounts: IndexedSeq[Account])


object NxtSimulator extends App {
  val MinersCount = 800

  def generateNormalState() = {
    val balances = Distribution.exponential(50).sample(MinersCount).map(_ * 100000000).map(_.toInt)

    val accs = balances.map { b =>
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      Account(b, pk)
    }.toIndexedSeq

    NxtBlockchainState(Map(1 -> GenesisBlock), 1, accs)
  }

  def generateAttackerState(normalState: NxtBlockchainState, attackerPercent: Int, accountsNum: Int) = {
    val totalBalance = normalState.accounts.map(_.balance.toLong).sum
    val attBalance = ((totalBalance * attackerPercent) / 100).toInt
    val accs = (1 to accountsNum).map { _:Int =>
      val pk = new Array[Byte](32)
      Random.nextBytes(pk)
      Account(attBalance / accountsNum, pk)
    }.toIndexedSeq

    NxtBlockchainState(Map(1 -> GenesisBlock), 1, accs)
  }

  def generateBlocks(initState: NxtBlockchainState, howMany: Int): NxtBlockchainState =
    1.to(howMany).foldLeft(initState) { case (blockchainState, _) =>
      val accs = blockchainState.accounts
      val h = blockchainState.height
      val last = blockchainState.blockchain.get(h).get
      val lastGs = last.generationSignature
      val lastBt = last.baseTarget
      val lastTime = last.time

      val bestBlock = accs.foldLeft(None: Option[NxtBlock]) { case (blockOpt, acc) =>
        val gs = HashImpl.hash(lastGs ++ acc.publicKey)
        val hitTime = lastTime + (BigInt(1, HashImpl.hash(gs).take(8)).toDouble / (lastBt * acc.balance)).ceil.toInt

        if (hitTime < blockOpt.map(_.time.toInt).getOrElse(Int.MaxValue)) {
          val delta = hitTime - lastTime
          val btd = lastBt * delta / 60
          val btMin = lastBt/2
          val btMax = lastBt*2

          val bt = Math.max(Math.min(btd, btMax), btMin)

          Some(NxtBlock(h + 1, hitTime, gs, bt, acc))
        } else blockOpt
      }.get

      //println(bestBlock.height + "::" + bestBlock.time)
      NxtBlockchainState(blockchainState.blockchain.updated(h+1, bestBlock), height = h+1, accounts = accs)
    }

  def cumdef(blockchainState: NxtBlockchainState) = blockchainState.blockchain.values.map(b => Long.MaxValue/b.baseTarget).sum


  def printAnalysis(blockchainState: NxtBlockchainState): Unit ={
    val chain = blockchainState.blockchain.drop(3)
    val balances = blockchainState.accounts.map(_.balance.toLong)
    val total = balances.sum

    println("Blocks generated: " + chain.size)
    println("balances:" + balances)
    println("Big guys: " + balances.filter(_ > 10000000))

    val blocks: Seq[NxtBlock] = chain.values.toSeq
    //out balance -> number of tickets
    val stats = blocks.map(b => b.generator -> 1).groupBy(_._1).map { case (a, s) =>
      100 * a.balance.toDouble / total -> 100 * s.map(_._2).size.toDouble / blocks.size
    }.toSeq.sortBy(_._1)
    println("Accounts generated tickets: " + stats.size)
    println(stats.mkString("\n"))
  }


  //printAnalysis(generateBlocks(generateNormalState(), 20000))


  val BlocksToGenerate = 10
  val Experiments = 10000

  val c = ((1 to Experiments) map {_ :Int =>
    val ns = generateNormalState()
    val cn = cumdef(generateBlocks(ns, BlocksToGenerate))

    val as = generateAttackerState(ns, 40, 20)
    val ca = cumdef(generateBlocks(as, BlocksToGenerate))
    println("normal cd: " + cn)
    println("attack cd: " + ca)
    println("success: " + (cn < ca))
    cn < ca
  }).count(_ == true)

  println("Total success: " + c.toDouble/Experiments*100)

  //60 - 0.11
  //10
}
