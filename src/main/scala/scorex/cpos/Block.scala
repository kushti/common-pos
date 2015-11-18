package scorex.cpos

import TypesAndConstants._

sealed trait BlockLike {
  val seed: Array[Byte]
}

case class PreBlock(override val seed: Array[Byte]) extends BlockLike

case class PreBlock1(epochTime: Time,
                     generator1: Account,
                     override val seed: Array[Byte]) extends BlockLike

case class PreBlock2(epochTime: Time,
                     generator1: Account,
                     generator2: Account,
                     override val seed: Array[Byte]) extends BlockLike

//todo: Seq[Account] with retargeting based on a seq size ?
case class Block(epochTime: Time,
                 generator1: Account,
                 generator2: Account,
                 generator3: Account,
                 override val seed: Array[Byte]) extends BlockLike