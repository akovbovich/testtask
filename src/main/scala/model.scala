object model {
  type Tick = (Time, Price)
  type NEL[A] = ::[A]
  final val NEL = ::

  case class Price(value: Long) extends AnyVal

  case class Time(epoch: Long) extends AnyVal with Ordered[Time] {
    def +(duration: Duration): Time = Time(epoch + duration.seconds)
    def compare(that: Time): Int = epoch.compareTo(that.epoch)
  }

  case class Duration(seconds: Long) extends AnyVal

  implicit def longToPrice(value: Long): Price = Price(value)

  implicit def priceToLong(price: Price): Long = price.value

  case class OHLC(
      open: Price,
      high: Price,
      low: Price,
      close: Price,
      start: Time,
      duration: Duration
  ) {
    override def toString: String =
      s"OHLC(open=${open.value},high=${high.value},low=${low.value},close=${close.value})"
  }

  case class OHLCData(startTime: Time, duration: Duration, ticks: NEL[Tick])

  object OHLC {

    def apply(data: OHLCData): OHLC =
      apply(data.ticks, data.startTime, data.duration)

    def apply(
        ticks: NEL[Tick],
        startTime: Time,
        duration: Duration
    ): OHLC = {
      ticks match {
        case (_, px) :: Nil => OHLC(px, px, px, px, startTime, duration)
        case (_, px) :: _ =>
          val (lowPx, highPx, closePx) =
            ticks.foldLeft((px, px, px)) { case ((min, max, _), (_, px)) =>
              (Math.min(min, px), Math.max(max, px), px)
            }
          OHLC(px, highPx, lowPx, closePx, startTime, duration)
      }
    }
  }
}
