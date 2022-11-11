package net.pawel.events

import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.CollectionConverters._
import scala.jdk.CollectionConverters._

object Facebook extends FetchPage {
  private val eventBriteUrl = """https://(fb\.me|www\.facebook\.com)/.+""".r

  private def isFacebookUrl(url: String): Boolean = eventBriteUrl.matches(url)

  def fetchCurrentEvents(allUrls: List[String]) = {
    val parallel = allUrls.par

    val forkJoinPool = new java.util.concurrent.ForkJoinPool(1000)
    parallel.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

    val facebookUrls = parallel
      .filter(isFacebookUrl)
      .filter(isEventUrl)
      .distinct
      .map(fetchPage)

    println(facebookUrls.mkString("\n"))
  }

  private def isEventUrl(url: String): Boolean =
    url.contains("/e/") || url.contains("/events/")
}
