package dst.lib.concurrent

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import scala.collection.GenTraversableOnce
import scala.collection.GenTraversableLike
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

import scala.util.Try
import scala.util.Success

import scala.collection.mutable.Builder
import scala.collection.generic.CanBuildFrom

object TolerantFutureCollection {
  private def mapValue[T](f: Future[T])(implicit context: ExecutionContext): Future[Try[T]] = {
    val p = Promise[Try[T]]()
    f onComplete p.success
    p.future
  }

  /**
   * Transforms a `GenTraversableOnce[A]` into a `Future[GenTraversableOnce[B]]` using the provided function `A => Future[B]`.
   * The returned collection will eventual value contains the results of each future.
   *
   * <pre>
   * {@code
   * val futureList = (TolerantFuture.traverse(List(2, 4, 0, 5)) {x => Future(100 / x)}) map { f =>
   *   f foreach {
   *     case Failure(t) => println(s"Could not compute: ${t}")
   *     case _ =>
   *   }
   *   f collect { case Success(x) => x }
   * }
   * val seqSum = Await.result(futureList.map(_.sum), 1.second).asInstanceOf[Int]
   * seqSum must be(95)
   * </pre>
   */
  def traverse[A, B, M[X] <: GenTraversableOnce[X]](in: M[A])(fn: (A) ⇒ Future[B])(implicit cbf: CanBuildFrom[M[A], Try[B], M[Try[B]]], executor: ExecutionContext): Future[M[Try[B]]] = {
    in.foldLeft(Future.successful(cbf(in))) { (fr, a) =>
      val fb = fn.andThen(mapValue)(a)
      for (r <- fr; b <- fb) yield (r += b)
    }.map(_.result())
  }

  /**
   * Transforms a `GenTraversableOnce[A]` into a `Future[GenTraversableOnce[B]]` using the provided function `A => Future[B]`.
   * The returned collection will eventual value contains the results of each successful future and the failed
   * futures will be discarded.
   *
   * <pre>
   * {@code
   * val futureList = TolerantFuture.traverseSuccess(List(2, 4, 0, 5)) {x => Future(100 / x)}
   * val seqSum = Await.result(futureList.map(_.sum), 1.second).asInstanceOf[Int]
   * seqSum must be(95)
   * </pre>
   */
  def traverseSuccess[A, B, M[X] <: GenTraversableLike[X, M[X]]](in: M[A])(fn: (A) ⇒ Future[B])(implicit cbfa: CanBuildFrom[M[A], Try[B], M[Try[B]]], cbfb: CanBuildFrom[M[Try[B]], B, M[B]], executor: ExecutionContext): Future[M[B]] = {
    val fr = traverse(in)(fn)(cbfa, executor)
    fr.map (_ collect{ case Success(x) => x })
  }

  /**
   * Transforms a `GenTraversableOnce[Future[A]]` into a `Future[GenTraversableOnce[A]]`.
   * Useful for reducing many `Future`s into a single `Future` and make sure all successful futures are executed.
   * The returned collection will eventual value contains the results of each future.
   *
   * <pre>
   * {@code
   * val futureList = TolerantFuture.sequence(List(2, 4, 0, 5) map { x => Future(100 / x) }) map { _ collect {case Success(x) => x } }
   * val seqSum = Await.result(futureList.map(_.sum), 1.second).asInstanceOf[Int]
   * seqSum must be(95)
   * </pre>
   */
  def sequence[A, M[X] <: GenTraversableOnce[X]](in: M[Future[A]])(implicit cbf: CanBuildFrom[M[Future[A]], Try[A], M[Try[A]]], executor: ExecutionContext): Future[M[Try[A]]] = {
    in.foldLeft(Future.successful(cbf(in))) { (fr, fa) =>
      for (r <- fr; a <- mapValue(fa)) yield (r += a)
    } map (_.result())
  }

  /**
   * Transforms a `GenTraversableOnce[Future[A]]` into a `Future[GenTraversableOnce[A]]`.
   * Useful for reducing many `Future`s into a single `Future` and make sure all successful futures are executed.
   * The returned collection will eventual value contains the results of each successful future and the failed
   * futures will be discarded.
   *
   * <pre>
   * {@code
   * val futureList = TolerantFuture.sequenceSuccess(List(2, 4, 0, 5) map { x => Future(100 / x) })
   * val seqSum = Await.result(futureList.map(_.sum), 1.second).asInstanceOf[Int]
   * seqSum must be(95)
   * </pre>
   */
  def sequenceSuccess[A, M[X] <: GenTraversableLike[X, M[X]]](in: M[Future[A]])(implicit cbfa: CanBuildFrom[M[Future[A]], Try[A], M[Try[A]]], cbfb: CanBuildFrom[M[Try[A]], A, M[A]], executor: ExecutionContext): Future[M[A]] = {
    sequence(in).map (_ collect { case Success(x) => x })
  }
}
