package cpos.model

import cpos.model.TypesAndConstants._

import scala.util.Random

//todo: add signatures?
case class Block(height: Int,
                 time: Time,
                 seed: Array[Byte],
                 ticket1: Ticket1, //todo: Seq[Account] with retargeting based on a seq size ?
                 ticket2: Ticket2, //todo: Seq[Account] with retargeting based on a seq size ?
                 ticket3: Ticket3,
                 generator: Account) {
  lazy val score: BigInt = ticket1.score + ticket2.score + ticket3.score
}

object GenesisCreator extends Account(0, Array.fill(PubKeyLength)(0))

class GenesisBlock(override val height: Int) extends Block(
  height,
  0L,
  seed = Array.fill(SeedLength)(Random.nextInt(255).toByte),
  ticket1 = GenesisBlock.genesisTicket1(),
  ticket2 = GenesisBlock.genesisTicket2(),
  ticket3 = GenesisBlock.genesisTicket3(),
  generator = GenesisCreator
)

object GenesisBlock{
  def genesisTicket1() = Ticket1(Array.fill(SeedLength)(Random.nextInt(255).toByte), GenesisCreator)
  def genesisTicket2() = Ticket2(Array.fill(SeedLength)(Random.nextInt(255).toByte), GenesisCreator)
  def genesisTicket3() = Ticket3(Array.fill(SeedLength)(Random.nextInt(255).toByte), GenesisCreator)
}