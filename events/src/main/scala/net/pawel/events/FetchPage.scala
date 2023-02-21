package net.pawel.events

import kong.unirest.Unirest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait FetchPage {
  def fetchUrl(url: String): String

  def apply(url: String): Document = {
    try {
      Jsoup.parse(fetchUrl(url))
    } catch {
      case _: Throwable => null
    }
  }
}

class FetchPageWithUnirest extends FetchPage {
  def fetchUrl(url: String): String = {
    try {
      Unirest.get(url).asString().getBody
    } catch {
      case _: Throwable => null
    }
  }
}
