package model

case class Timestamp(epoch: Long) extends AnyVal with Ordered[Timestamp] {
  def +(duration: Duration): Timestamp = Timestamp(epoch + duration.seconds)

  def -(rhs: Timestamp): Duration = Duration(epoch) - Duration(rhs.epoch)

  def compare(that: Timestamp): Int = epoch.compareTo(that.epoch)
}

object Timestamp {
  final val Zero = Timestamp(0)
}