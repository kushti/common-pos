package cpos.model

import cpos.model.TypesAndConstants._

case class Account(balance: Long, publicKey: PublicKey) {
  require(balance >= 0)
  require(publicKey.length == PubKeyLength)
}