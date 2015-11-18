package scorex.cpos

import scorex.cpos.TypesAndConstants._

case class Account(balance: Long, publicKey: PublicKey) {
  require(balance >= 0)
  require(publicKey.length == PubKeyLength)
}