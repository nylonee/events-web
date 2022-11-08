package net.pawel.events

import kong.unirest.Unirest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait FetchPage {
  protected def fetchPage(url: String): Document = {
    try {
      val body = Unirest.get(url).asString().getBody
      Jsoup.parse(body)
    } catch {
      case e => null
    }
  }
}
