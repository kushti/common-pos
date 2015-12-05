package cpos.model

import cpos.util.HashImpl

object TypesAndConstants{
  type Time = Long
  type PublicKey = Array[Byte]

  type BlockChain = IndexedSeq[Block]

  val PuzLength = HashImpl.DigestSize
  val PubKeyLength = 32
}