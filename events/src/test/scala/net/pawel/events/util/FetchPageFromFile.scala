package net.pawel.events.util

import net.pawel.events.FetchPage
import net.pawel.events.util.PathFromUrl.makePathFromUrl

import scala.io.Source

class FetchPageFromFile extends FetchPage {
  override def fetchUrl(url: String): String = {
    val path: String = makePathFromUrl(url)
    val stream = classOf[FetchPageFromFile].getResourceAsStream(path)
    if (stream == null)
      null
    else
      Source.fromInputStream(stream).getLines().mkString("\n")
  }
}




