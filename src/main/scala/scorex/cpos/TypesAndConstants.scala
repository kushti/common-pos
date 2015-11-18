package scorex.cpos

object TypesAndConstants{
  type Time = Long
  type PublicKey = Array[Byte]

  type BlockChain = List[Block]

  val SeedLength = 32
  val PubKeyLength = 32
}