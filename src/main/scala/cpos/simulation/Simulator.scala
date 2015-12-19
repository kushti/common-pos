package cpos.simulation

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import cpos.actors.{Miner, MinerSpec}
import cpos.model.{Block, Ticket}
import cpos.simulation.SimulatorSpec.NewTick

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Event


class Simulator extends Actor with ActorLogging {

  import SimulatorSpec._

  var time = 0

  val EndTime = 20000

  val MinersCount = 300

  val balances = (1 to MinersCount).map(_ => new SecureRandom().nextInt(50000000)).toSeq
  val miners = balances.map(balance => context.system.actorOf(Props(classOf[Miner], self, balance)))

  override def receive = {
    case NewTick =>
      time == EndTime match {
        case true =>
          val total = balances.map(_.toLong).sum
          miners.head ! MinerSpec.AnalyzeChain(total)
        case false =>
          time = time + 1
          log.info(s"Time $time, going to inform miners about clocks update")
          miners.foreach(ref => ref ! MinerSpec.TimerUpdate(time))
      }

    case t: Ticket =>
      miners.foreach(ref => ref ! t)

    case b: Block =>
      log.info("New block: " + b)
      miners.foreach(ref => ref ! b)

    case nonsense: Any =>
      log.warning(s"Got strange input: $nonsense")
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
