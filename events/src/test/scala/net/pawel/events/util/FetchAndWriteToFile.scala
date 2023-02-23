package net.pawel.events.util

import net.pawel.events.util.PathFromUrl.makePathFromUrl
import net.pawel.events.{FetchPage, FetchPageWithUnirest}

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class FetchAndWriteToFile extends FetchPage {
  private val fetchPage = new FetchPageWithUnirest

  override def fetchUrl(url: String): String = {
    val path = makePathFromUrl(url)
    val file = new File("events/src/test/resources/" + path)
    val parentFile = file.getParentFile
    Option(fetchPage.fetchUrl(url)).map(pageContent => {
      parentFile.mkdirs()
      Files.write(file.toPath, pageContent.getBytes(StandardCharsets.UTF_8))
      pageContent
    }).getOrElse(null)
  }
}
