package cpos.model

import cpos.model.TypesAndConstants._

//todo: add signatures?
case class Block(time: Time,
                 puz: Array[Byte],
                 ticket1: Ticket, //todo: Seq[Account] with retargeting based on a seq size ?
                 ticket2: Ticket, //todo: Seq[Account] with retargeting based on a seq size ?
                 ticket3: Ticket,
                 generator: Account) {
  lazy val score: BigInt = BigInt(ticket1.score) * ticket2.score * ticket3.score
}

object GenesisCreator extends Account(0, Array.fill(PubKeyLength)(0))

object GenesisTicket1 extends Ticket1(Array.fill(PuzLength)(0), GenesisCreator)

object GenesisTicket2 extends Ticket2(Array.fill(PuzLength)(0), GenesisCreator)

object GenesisTicket3 extends Ticket3(Array.fill(PuzLength)(0), GenesisCreator)

object GenesisBlock extends Block(0L,
  puz = Array.fill(PuzLength)(0),
  ticket1 = GenesisTicket1,
  ticket2 = GenesisTicket2,
  ticket3 = GenesisTicket3,
  generator = GenesisCreator
)