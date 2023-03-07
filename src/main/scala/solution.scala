import model._

object solution {

  // plain solution
  def makeOHLC(ticks: List[Tick], duration: Duration): List[OHLC] = {
    @scala.annotation.tailrec
    def loop(ticks: List[Tick], ohlcs: List[OHLC]): List[OHLC] = {
      ticks match {
        case (startTime, _) :: _ =>
          val (current, rest) = ticks.partition { case (time, _) =>
            val endTime = startTime + duration
            startTime <= time && time <= endTime
          }
          val ohlc = OHLC(NEL(current.head, current.tail), startTime, duration)
          loop(rest, ohlc :: ohlcs)
        case Nil => ohlcs.reverse
      }
    }

    loop(ticks, List.empty)
  }

  // another solution using foldLeft, but not as clear as above one
  def makeOHLC_2(ticks: List[Tick], duration: Duration): List[OHLC] = {
    ticks match {
      case (startTime, _) :: _ =>
        val (_, list) =
          ticks.foldLeft(startTime + duration -> List.empty[OHLCData]) {
            case ((endTime, list), tick) =>
              tick match {
                case (t, _) if t <= endTime =>
                  list match {
                    case data :: rest =>
                      val newData = data.copy(ticks = NEL(tick, data.ticks))
                      endTime -> (newData :: rest)
                    case Nil =>
                      endTime -> List(OHLCData(t, duration, NEL(tick, Nil)))
                  }
                case (t, _) =>
                  val data = OHLCData(t, duration, NEL(tick, Nil))
                  (t + duration) -> (data :: list)
              }
          }

        list.reverse.map { data =>
          val NEL(tick, ticks) = data.ticks.reverse
          OHLC(NEL(tick, ticks), data.startTime, data.duration)
        }

      case Nil => List.empty
    }
  }
}
