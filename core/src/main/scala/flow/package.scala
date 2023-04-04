import akka.stream.scaladsl.Flow

package object flow {
  implicit def flowOps[In, Out, Mat](flow: Flow[In, Out, Mat]) = new FlowOps(flow)
}
