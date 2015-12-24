package cpos.model

import cpos.util.HashImpl

object TypesAndConstants{
  type Time = Long
  type PublicKey = Array[Byte]

  type BlockChain = scala.collection.mutable.Buffer[Block]

  val SeedLength = HashImpl.DigestSize
  val PubKeyLength = 32
}