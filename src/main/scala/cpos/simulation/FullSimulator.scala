package cpos.simulation

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import cpos.actors.{Miner, MinerSpec}
import cpos.model.{Block, Ticket}
import cpos.simulation.FullSimulatorSpec.NewTick
import probability_monad.Distribution

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Event


class FullSimulator extends Actor with ActorLogging {

  import FullSimulatorSpec._

  var time = 0

  val EndTime = 10000

  val MinersCount = 2048

  val balances = Distribution.exponential(50).sample(MinersCount).map(_ * 100000000).map(_.toInt)
  val miners = balances.map(balance => context.system.actorOf(Props(classOf[Miner], self, balance)))

  override def receive = {
    case NewTick =>
      time == EndTime match {
        case true =>
          val total = balances.map(_.toLong).sum
          miners.head ! MinerSpec.AnalyzeChain(balances, total)
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

object FullSimulatorSpec {
  case object NewTick
}


object FullSimulatorLauncher {

  def main(args: Array[String]): Unit = {

    //println(Distribution.exponential(50).sample(2048).map(_ * 100000000).map(_.toInt).filter(_ > 10000000))

    val system = ActorSystem()
    val simulator = system.actorOf(Props[FullSimulator])
    system.scheduler.schedule(0.seconds, 300.millis)(simulator ! NewTick)
  }
}
