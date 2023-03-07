import model._
import solution._

import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val values =
      List.tabulate(8)(n => Time(n) -> Price(Random.between(-10, 10)))
    val candles = makeOHLC(values, Duration(1))
    println(
      values.map { case (t, px) => s"(${t.epoch},${px.value})" }.mkString(",")
    )
    candles.foreach(println)
    check()
  }

  private def check(): Unit = {
    assert(
      makeOHLC(List.empty, Duration(1)) == makeOHLC_2(List.empty, Duration(1))
    )
    assert(
      makeOHLC(List.empty, Duration(0)) == makeOHLC_2(List.empty, Duration(0))
    )
    assert(
      makeOHLC(List(Time(0) -> Price(0)), Duration(1)) == makeOHLC_2(
        List(Time(0) -> Price(0)),
        Duration(1)
      )
    )
    val values = List.tabulate(1000)(n => Time(n) -> Price(Random.nextLong))
    assert(
      makeOHLC(values, Duration(60)) == makeOHLC_2(values, Duration(60))
    )
  }
}
