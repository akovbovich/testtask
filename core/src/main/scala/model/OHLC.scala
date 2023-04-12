package model

final case class OHLC(
    open: Price,
    high: Price,
    low: Price,
    close: Price,
    openTs: Timestamp,
    closeTs: Timestamp,
    interval: Duration
) {
  override def toString: String =
    s"OHLC(${open.value},${high.value},${low.value},${close.value},period=[${openTs.epoch},${closeTs.epoch}),interval=${interval.seconds})"
}

object OHLC {
  final class Aggregator(interval: Duration) {
    private var current: Option[OHLC] = None
    private var remainingDuration: Duration = interval

    require(interval.seconds > 0, "Interval should be > 0")

    private def openIntervalTs(tickTs: Timestamp): Timestamp =
      (0L to tickTs.epoch by interval.seconds)
        .map(Timestamp.apply)
        .lastOption
        .getOrElse(Timestamp.Zero)

    private def closeIntervalTsExclusive(tickTs: Timestamp): Timestamp =
      openIntervalTs(tickTs) + interval

    def addTick(tick: Tick): Boolean = {
      current match {
        case Some(ohlc) if ohlc.openTs <= tick.ts && tick.ts < ohlc.closeTs =>
          val updated = ohlc.copy(
            high = Price.max(ohlc.high, tick.price),
            low = Price.min(ohlc.low, tick.price),
            close = tick.price
          )
          current = Some(updated)
          remainingDuration = updated.closeTs - tick.ts
          true

        case None =>
          val initial = OHLC(
            open = tick.price,
            high = tick.price,
            low = tick.price,
            close = tick.price,
            openTs = openIntervalTs(tick.ts),
            closeTs = closeIntervalTsExclusive(tick.ts),
            interval = interval
          )
          current = Some(initial)
          remainingDuration = initial.closeTs - tick.ts
          true

        case _ =>
          remainingDuration = Duration.Zero
          false
      }
    }

    def touch(duration: Duration): Unit = remainingDuration -= duration

    def isIntervalClosed: Boolean = remainingDuration == Duration.Zero

    def nonEmpty: Boolean = current.nonEmpty

    def get: OHLC = current.get
  }
}
