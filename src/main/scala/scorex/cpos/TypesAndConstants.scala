package scorex.cpos

object TypesAndConstants{
  type Time = Long
  type PublicKey = Array[Byte]

  type BlockChain = IndexedSeq[Block]

  val PuzLength = HashImpl.DigestSize
  val PubKeyLength = 32
}