import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.IODispatcher
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Attributes}
import flow._
import model._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object ConsoleStreamDemo extends App {
  implicit val as = ActorSystem("console-tick-stream")
  implicit val mat = ActorMaterializer

  val intervalSecondsArg = args.headOption.flatMap(_.toIntOption).getOrElse(5)

  def parseTick(s: String): Tick = {
    val ts :: px :: Nil = s.split(',').take(2).map(_.trim).toList
    Tick(Timestamp(ts.toLong), Price(BigDecimal(px)))
  }

  val stdin: Source[Tick, NotUsed] =
    Source
      .unfold(())(_ => Some(() -> Option(io.StdIn.readLine()).getOrElse("")))
      .map(parseTick)
      .recoverWith { case _ => stdin }
      .withAttributes(Attributes(IODispatcher))

  def emitter(
      intervalSeconds: Long,
      clockSeconds: Long
  ): Flow[Tick, OHLC, NotUsed] = {
    type T = (OHLC.Aggregator, Option[OHLC])

    def allocate = new OHLC.Aggregator(Duration(intervalSeconds))

    val aggregate: (OHLC.Aggregator, Tick) => T = { case (agg, tick) =>
      if (agg.addTick(tick)) agg -> None
      else {
        val newAgg = allocate
        newAgg.addTick(tick)
        newAgg -> Some(agg.get)
      }
    }

    val emitOnTimer: (OHLC.Aggregator => T, FiniteDuration) = (
      { agg =>
        agg.touch(Duration(clockSeconds))
        if (agg.isIntervalClosed && agg.nonEmpty) allocate -> Some(agg.get)
        else agg -> None
      },
      FiniteDuration(clockSeconds, TimeUnit.SECONDS)
    )

    Flow[Tick].aggregateWithTimer(allocate _)(aggregate, emitOnTimer)
  }

  val stdout = Sink.foreach(println)

  println(
    "input timestamp, price pairs separated by newlines, e.g. 1,100\\n2,110"
  )

  stdin.via(emitter(intervalSecondsArg, 1)).to(stdout).run()
}
