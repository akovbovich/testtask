package model

case class Price(value: BigDecimal) extends AnyVal

object Price extends Ordering[Price] {
  final val Zero = Price(0)
  def compare(x: Price, y: Price): Int = x.value.compare(y.value)
}
