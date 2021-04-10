package bekindrewind

import bekindrewind.uril.Generators
import munit._
import org.scalacheck.Prop._

import java.net.URI

class VcrMatcherSpec extends ScalaCheckSuite {

  property("VcrMatcher.identity should allow all requests") {
    forAll(Generators.vcrRecordRequest) { (req: VcrRecordRequest) =>
      assert(VcrMatcher.identity.shouldRecord(req))
    }
  }

  test("VcrMatcher#filter") {
    val matcher = VcrMatcher.identity.filter(_.method == "GET")

    assert(matcher.shouldRecord(VcrRecordRequest("GET", new URI("https://example.com"), "", Map.empty, "HTTP/1.1")))
    assert(!matcher.shouldRecord(VcrRecordRequest("POST", new URI("https://example.com"), "", Map.empty, "HTTP/1.1")))
  }

  test("VcrMatcher#filter (append)") {
    val uriA     = new URI("https://a.com")
    val uriB     = new URI("https://b.com")
    val matcher  = VcrMatcher.default.filter(_.uri == uriA)
    val matcher2 = VcrMatcher.default.filter(_.uri == uriB)

    val combinedMatcher = matcher.append(matcher2)

    val requestA = VcrRecordRequest("GET", uriA, "", Map.empty, "HTTP/1.1")

    assert(matcher.shouldRecord(requestA))
    assert(!matcher2.shouldRecord(requestA))

    assert(matcher.group(requestA) == VcrKey("GET", uriA))
    assert(matcher2.group(requestA) == VcrKey.Ungrouped)

    assert(combinedMatcher.group(requestA) == VcrKey("GET", uriA))

    // If the 1st matcher does not match, it should move on to the 2nd matcher
    val requestB = VcrRecordRequest("GET", uriB, "", Map.empty, "HTTP/1.1")

    assert(!matcher.shouldRecord(requestB))
    assert(matcher2.shouldRecord(requestB))

    assert(matcher.group(requestB) == VcrKey.Ungrouped)
    assert(matcher2.group(requestB) == VcrKey("GET", uriB))

    assert(combinedMatcher.group(requestB) == VcrKey("GET", uriB))
  }
}
