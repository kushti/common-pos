package cpos.model

import cpos.util.HashImpl.hash

sealed trait Ticket {

  val blockPuz: Array[Byte]
  val account: Account

  val byteNum: Byte

  lazy val score: BigInt = {
    val m = java.lang.Byte.toUnsignedInt(hash(account.publicKey ++ blockPuz)(byteNum))
    if (m >= 64) 0 else BigInt(m).pow(8) * account.balance
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