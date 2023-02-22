package net.pawel.events.util

object PathFromUrl {
  def makePathFromUrl(url: String) = {
    val replaced = url.replace("://", "/")
    val path = removeTrailingSlash("/" + replaced)
    path + ".content"
  }

  private def removeTrailingSlash(path: String) = {
    if (path.endsWith("/")) path.substring(0, path.length - 1) else path
  }
}
