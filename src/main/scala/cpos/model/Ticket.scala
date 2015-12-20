package cpos.model

import cpos.util.HashImpl.hash

sealed trait Ticket {

  val blockPuz: Array[Byte]
  val account: Account

  val byteNum: Byte

  /*
    private def log2nlz(bits:Int) =
      if( bits == 0 ) 0 else 31 - Integer.numberOfLeadingZeros( bits )
    */

  lazy val score: BigInt = Ticket.score(blockPuz, account, byteNum)

  override def toString: String = s"Ticket$byteNum (score: $score) by $account"
}

object Ticket{
  def score(blockPuz: Array[Byte], account: Account, byteNum: Byte):BigInt = {
    val m = java.lang.Byte.toUnsignedInt(hash(account.publicKey ++ blockPuz)(byteNum))
    val b = account.balance
    if (m > 16) 0 else BigInt(b).pow(m)
  }
}


case class Ticket1(override val blockPuz: Array[Byte],
                   override val account: Account) extends Ticket {
  override val byteNum: Byte = 1
}

case class Ticket2(override val blockPuz: Array[Byte],
                   override val account: Account) extends Ticket {
  override val byteNum: Byte = 2
}

case class Ticket3(override val blockPuz: Array[Byte],
                   override val account: Account) extends Ticket {
  override val byteNum: Byte = 3
}