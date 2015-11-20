package scorex.cpos

import TypesAndConstants._

//todo: add signatures
sealed trait BlockLike {
  val seed: Array[Byte]
}

trait NotCompleted extends BlockLike

case class PreBlock1(time: Time,
                     generator1: Account,
                     override val seed: Array[Byte]) extends BlockLike with NotCompleted

case class PreBlock2(time: Time,
                     generator1: Account,
                     generator2: Account,
                     override val seed: Array[Byte]) extends BlockLike with NotCompleted


case class Block(time: Time,
                 generator1: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 generator2: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 generator3: Account, //todo: Seq[Account] with retargeting based on a seq size ?
                 override val seed: Array[Byte]) extends BlockLike

object GenesisCreator extends Account(0, Array.fill(PubKeyLength)(0))

object GenesisBlock extends Block(0L,
  generator1 = GenesisCreator,
  generator2 = GenesisCreator,
  generator3 = GenesisCreator,
  seed = Array.fill(SeedLength)(0)
)