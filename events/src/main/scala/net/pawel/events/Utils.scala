package net.pawel.events

import java.util.concurrent.ForkJoinPool
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.immutable.ParSeq
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Utils {
  def parallelize[T](list: Seq[T]): ParSeq[T] = {
    val parallel = list.par

    val forkJoinPool = new ForkJoinPool(1000)
    parallel.tasksupport = new ForkJoinTaskSupport(forkJoinPool)
    parallel
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  def await[T](future: Future[T]): T = Await.result(future.recover {
    case t => throw t
  }, Duration.Inf)
}
