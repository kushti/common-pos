package scorex.simulation

import akka.actor.{Props, Actor, ActorSystem}
import scorex.actors.Miner

trait Event


class Simulator extends Actor {

  import SimulatorSpec._

  var time = 0

  val MaxDelta = 10

  val MinersCount = 100

  val packetsLossPercentage = 1

  //todo: pass delta?
  val miners = 1.to(100).toSeq.map(_ => context.system.actorOf(Props[Miner]))

  override def receive = {
    case NewTick =>
      time = time + 1
  }
}

object SimulatorSpec {
  case object NewTick
}


object SimulatorLauncher {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem()

    val simulator = system.actorOf(Props[Simulator])
  }
}
