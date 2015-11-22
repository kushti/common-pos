package scorex.cpos

import java.security.MessageDigest

import scorex.cpos.TypesAndConstants._

case class GenerationRequest(account: Account, right: Long)


object CposFunctions {

  val DigestSize = 32

  def round(lastBlock: Block, time: Time): Option[Round] = {
    time - lastBlock.time > 30 match {
      case true => Some(FirstRound)
      case false => None
    }
  }

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
        val second = java.lang.Byte.toUnsignedInt(h.tail.head)
        val third = java.lang.Byte.toUnsignedInt(h.tail.tail.head)

        first < 32 match {
          case true =>
            val r = account.balance * first * second * third
            Some(GenerationRequest(account, r))

          case false =>
            None
        }

      case SecondRound =>
        raw match {
          case PreBlock1(time, gen1, seed) =>
            val h = hash(seed ++ gen1.publicKey ++ account.publicKey)
            val first = java.lang.Byte.toUnsignedInt(h.head)
            val second = java.lang.Byte.toUnsignedInt(h.tail.head)

            first < 32 match {
              case true =>
                val r = account.balance * first * second
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