package bekindrewind.playwsstandalone

import akka.stream.Materializer
import bekindrewind.{ RecordOptions, VcrClient, VcrMatcher }
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSRequest }

import java.nio.file.Path
import scala.util.Try

class VcrStandaloneWSClient(
  val underlyingClient: StandaloneWSClient,
  val recordingPath: Path,
  val recordOptions: RecordOptions = RecordOptions.default,
  val matcher: VcrMatcher = VcrMatcher.groupBy(r => (r.method, r.uri))
)(implicit val materializer: Materializer)
    extends StandaloneWSClient
    with VcrClient {

  override def underlying[T]: T =
    underlyingClient.underlying[T]

  override def url(url: String): StandaloneWSRequest =
    new VcrStandaloneWSRequest(underlyingClient.url(url), this)

  override def close(): Unit = {
    Try(save()).failed.foreach(_.printStackTrace())
    underlyingClient.close()
  }
}
