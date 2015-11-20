package scorex.cpos

import TypesAndConstants._

//todo: add signatures
sealed trait BlockLike {
  val seed: Array[Byte]
}

trait NotCompleted extends BlockLike

case class PreBlock1(epochTime: Time,
                     generator1: Account,
                     override val seed: Array[Byte]) extends BlockLike with NotCompleted

case class PreBlock2(epochTime: Time,
                     generator1: Account,
                     generator2: Account,
                     override val seed: Array[Byte]) extends BlockLike with NotCompleted


case class Block(epochTime: Time,
                 generator1: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 generator2: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 generator3: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 override val seed: Array[Byte]) extends BlockLike