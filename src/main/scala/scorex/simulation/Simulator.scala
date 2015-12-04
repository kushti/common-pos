package scorex.simulation

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scorex.actors.{Miner, MinerSpec}
import scorex.cpos.{Block, Ticket}
import scorex.simulation.SimulatorSpec.NewTick

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Event


class Simulator extends Actor with ActorLogging {

  import SimulatorSpec._

  var time = 0

  //val MaxDelta = 10

  val MinersCount = 100

  //val packetsLossPercentage = 1

  //todo: pass delta?
  val miners = 1.to(100).toSeq.map(_ => context.system.actorOf(Props[Miner]))

  val grs = mutable.Buffer[Ticket]()

  override def receive = {
    case NewTick =>
      time = time + 1
      miners.foreach(ref => ref ! MinerSpec.TimerUpdate(time))

    case t: Ticket =>
      grs += t
      log.info("Best ticket: " + grs.maxBy(_.right))
      log.info("size: " + grs.size + " others: " + grs)

    case b: Block =>

  }
}

object SimulatorSpec {

  case object NewTick

}


object SimulatorLauncher {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem()

    val simulator = system.actorOf(Props[Simulator])
    system.scheduler.schedule(0.seconds, 50.millis)(simulator ! NewTick)
  }
}
