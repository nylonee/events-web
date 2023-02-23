package net.pawel.events

import kong.unirest.Unirest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.util.Try

trait FetchPage {
  def fetchUrl(url: String): String

  def apply(url: String): Document = {
    Try(Jsoup.parse(fetchUrl(url))).getOrElse(null)
  }
}

class FetchPageWithUnirest extends FetchPage {
  def fetchUrl(url: String): String =
    Try(Unirest.get(url).asString()).filter(_.isSuccess).map(_.getBody).getOrElse(null)
}
