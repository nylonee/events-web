package net.pawel.events

import kong.unirest.Unirest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait FetchPage {
  protected def fetchPageAsString(url: String): String = {
    try {
      Unirest.get(url).asString().getBody
    } catch {
      case _: Throwable => null
    }
  }

  protected def fetchPage(url: String): Document = {
    try {
      Jsoup.parse(fetchPageAsString(url))
    } catch {
      case _: Throwable => null
    }
  }
}
