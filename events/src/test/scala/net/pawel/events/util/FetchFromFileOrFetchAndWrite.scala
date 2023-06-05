package net.pawel.events.util

import net.pawel.events.FetchPage

class FetchFromFileOrFetchAndWrite extends FetchPage {
  private val fetchPageFromFile = new FetchPageFromFile
  private val fetchAndWriteToFile = new FetchAndWriteToFile

  override def fetchUrl(url: String): String =
    Option(fetchPageFromFile.fetchUrl(url))
      .getOrElse(fetchAndWriteToFile.fetchUrl(url))
}
