package model

case class Duration(seconds: Long) extends AnyVal {
  def -(x: Duration): Duration = copy(Math.max(seconds - x.seconds, 0))
}

object Duration {
  final val Zero = Duration(0)
}
