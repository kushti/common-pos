package scorex.cpos

import java.security.MessageDigest

import scorex.cpos.TypesAndConstants._

case class GenerationRequest(account: Account, right: Long)

object CposFunctions {

  val DigestSize = 32

  def round(time: Time): Round = ???

  def hash(input: Array[Byte]): Array[Byte] =
    MessageDigest
      .getInstance("SHA-256")
      .digest(input)
      .ensuring(_.length == DigestSize)

  def checkRight(account: Account, raw: BlockLike, round: Round): Option[GenerationRequest] = {
    require(raw.seed.length == SeedLength)

    round match {
      case FirstRound =>
        require(raw.isInstanceOf[Block]) //previous block

        val h = hash(raw.seed ++ account.publicKey)
        val first = java.lang.Byte.toUnsignedInt(h.head)

        first < 64 match {
          case true =>
            val r = account.balance * first
            Some(GenerationRequest(account, r))

          case false =>
            None
        }

      case SecondRound =>
        raw match{
          case PreBlock1(time, gen1, seed) =>
            val h = hash(seed ++ gen1.publicKey ++ account.publicKey)
            val first = java.lang.Byte.toUnsignedInt(h.head)

            first < 64 match {
              case true =>
                val r = account.balance * first
                Some(GenerationRequest(account, r))

              case false =>
                None
            }

          case _ => None
        }

      case ThirdRound =>
        ???
    }
  }

  def generate() = ???
}


