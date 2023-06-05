package net.pawel.events

import com.github.takezoe.retry.{ExponentialBackOff, RetryPolicy, retry}
import kong.unirest.Unirest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.duration.DurationInt
import scala.util.Try

trait FetchPage {
  def fetchUrl(url: String): String

  def apply(url: String): Document = {
    Try(Jsoup.parse(fetchUrl(url))).getOrElse(null)
  }
}

class FetchPageWithUnirest extends FetchPage {
  def fetchUrl(url: String): String = {
    implicit val policy = RetryPolicy(
      maxAttempts = 5,
      retryDuration = 1.second,
      backOff = ExponentialBackOff,
      jitter = 1.second
    )

    retry {
      val result = Unirest.get(url).asString()
      if (!result.isSuccess) {
        null
      } else {
        result.getBody
      }
    }
  }
}
