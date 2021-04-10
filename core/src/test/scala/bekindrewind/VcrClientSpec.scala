package bekindrewind

import io.circe.parser._
import io.circe.syntax._
import munit._

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.time.OffsetDateTime

class VcrClientSpec extends FunSuite {

  test("Request and response are saved as JSON") {
    val recordingPath = Files.createTempFile("test", ".json")
    val client        = MockClient(recordingPath, RecordOptions.default, VcrMatcher.default)
    assert(client.previouslyRecorded.isEmpty)
    assert(client.newlyRecorded.get().isEmpty)

    val record = VcrRecord(
      VcrRecordRequest("GET", new URI("https://example.com/foo.json"), "{}", Map.empty, "HTTP/1.1"),
      VcrRecordResponse(200, "ok", Map.empty, "{}", Some("text/json")),
      OffsetDateTime.parse("2100-05-06T12:34:56.789Z")
    )
    client.newlyRecorded.set(Vector(record))
    client.save()
    assert(client.previouslyRecorded.isEmpty)
    assertEquals(client.newlyRecorded.get().size, 1)

    val savedJson = new String(Files.readAllBytes(recordingPath), StandardCharsets.UTF_8)
    val decoded   = decode[VcrRecords](savedJson).map(_.records)
    assertEquals(decoded, Right(Vector(record)))
  }

  test("Client loads the previous record when being constructed") {
    val record  = VcrRecord(
      VcrRecordRequest("GET", new URI("https://example.com/foo.json"), "{}", Map.empty, "HTTP/1.1"),
      VcrRecordResponse(200, "ok", Map.empty, "{}", Some("text/json")),
      OffsetDateTime.parse("2100-05-06T12:34:56.789Z")
    )
    val rawJson = VcrRecords(Vector(record), BuildInfo.version).asJson.spaces2

    val recordingPath = Files.createTempFile("test", ".json")
    Files.write(recordingPath, rawJson.getBytes(StandardCharsets.UTF_8))

    val client = MockClient(recordingPath, RecordOptions.default, VcrMatcher.groupBy(_ => "bucket"))
    assert(client.newlyRecorded.get().isEmpty)

    client.previouslyRecorded.get(VcrKey("bucket")) match {
      case None           => fail("Should load the record !!")
      case Some(previous) =>
        assertEquals(previous.records, Vector(record))
        assertEquals(previous.currentIndex.get(), 0)
    }
  }

}

final case class MockClient(recordingPath: Path, recordOptions: RecordOptions, matcher: VcrMatcher) extends VcrClient
