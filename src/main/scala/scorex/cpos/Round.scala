package scorex.cpos


sealed trait Round

case object FirstRound extends Round

case object SecondRound extends Round

case object ThirdRound extends Round