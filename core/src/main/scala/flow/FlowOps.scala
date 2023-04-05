package flow

import akka.stream.scaladsl.Flow
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

final class FlowOps[-In, +Out, +Mat](flow: Flow[In, Out, Mat]) {
  private type Repr[+O] = flow.Repr[O]

  def aggregateWithTimer[Agg, Emit](allocate: () => Agg)(
      aggregate: (Agg, Out) => (Agg, Option[Emit]),
      emitOnTimer: Option[(Agg => (Agg, Option[Emit]), FiniteDuration)]
  ): Repr[Emit] =
    flow.via(AggregateWithTimer(allocate, aggregate, emitOnTimer))
}

final case class AggregateWithTimer[In, Agg, Out](
    allocate: () => Agg,
    aggregate: (Agg, In) => (Agg, Option[Out]),
    emitOnTimer: Option[(Agg => (Agg, Option[Out]), FiniteDuration)]
) extends GraphStage[FlowShape[In, Out]] {

  private[this] var aggregated: Agg = null.asInstanceOf[Agg]

  private val in: Inlet[In] = Inlet[In]("AggregateWithTimer.in")
  private val out: Outlet[Out] = Outlet[Out]("AggregateWithTimer.out")

  override val shape: FlowShape[In, Out] = FlowShape(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      override def preStart(): Unit = {
        emitOnTimer.foreach { case (_, interval) =>
          scheduleAtFixedRate("AggregateWithTimer.Timer", interval, interval)
        }
      }

      override protected def onTimer(timerKey: Any): Unit = {
        emitOnTimer.foreach { case (isReadyOnTimer, _) =>
          if (aggregated != null) {
            val (updated, resultOpt) = isReadyOnTimer(aggregated)
            aggregated = updated
            resultOpt.foreach(Emit)
          }
        }
        if (isClosed(in)) completeStage()
      }

      override def onUpstreamFinish(): Unit = {}

      override def onPush(): Unit = {
        if (aggregated == null) aggregated = allocate()
        val (updated, resultOpt) = aggregate(aggregated, grab(in))
        aggregated = updated
        resultOpt.foreach(Emit)
        if (isAvailable(out)) pull(in)
      }

      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)

      setHandlers(in, out, this)

      private def Emit(value: Out): Unit = emit(out, value)
    }
}
