package net.pawel.events

import scala.util.matching.Regex.MatchIterator

object ExtractUrls {
  val UrlRegexp = """(http|ftp|https):\/\/([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:\/~+#-]*[\w@?^=%&\/~+#-])""".r

  def extractUrls(string: String): MatchIterator = {
    UrlRegexp.findAllIn(string)
  }
}
