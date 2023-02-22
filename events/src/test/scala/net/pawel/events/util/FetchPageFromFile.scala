package net.pawel.events.util

import kong.unirest.Unirest
import net.pawel.events.FetchPage
import net.pawel.events.util.PathFromUrl.makePathFromUrl

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.io.Source

class FetchPageFromFile extends FetchPage {
  override def fetchUrl(url: String): String = {
    val path: String = makePathFromUrl(url)
    Source.fromInputStream(classOf[FetchPageFromFile].getResourceAsStream(path)).getLines().mkString("\n")
  }
}

class FetchAndWriteToFile extends FetchPage {
  override def fetchUrl(url: String): String = {
    val path = makePathFromUrl(url)
    val file = new File("events/src/test/resources/" + path)
    val parentFile = file.getParentFile
    parentFile.mkdirs()
    val pageContent = Unirest.get(url).asString().getBody
    Files.write(file.toPath, pageContent.getBytes(StandardCharsets.UTF_8))
    pageContent
  }
}


