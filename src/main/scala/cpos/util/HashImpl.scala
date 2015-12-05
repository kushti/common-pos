package cpos.util

import java.security.MessageDigest

object HashImpl {
  val DigestSize = 32

  def hash(input: Array[Byte]): Array[Byte] =
    MessageDigest
      .getInstance("SHA-256")
      .digest(input)
      .ensuring(_.length == DigestSize)
}