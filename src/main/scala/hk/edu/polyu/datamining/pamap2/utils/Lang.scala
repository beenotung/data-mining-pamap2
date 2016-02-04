package hk.edu.polyu.datamining.pamap2.utils

import java.util.function.Consumer
import javafx.event.EventHandler
import javafx.util.Callback

import akka.actor.{ActorContext, ActorSystem}
import com.rethinkdb.gen.ast._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.language.implicitConversions

//import scala.collection.immutable.Range does not import for rethinkdb better support

/**
  * Created by beenotung on 1/25/16.
  */
object Lang {
  /*    Java Support    */
  implicit def runnable(fun: () => Unit): Runnable = new Runnable {
    override def run() = fun()
  }

  implicit def seqToList[A](seq: Seq[A]): java.util.List[A] = seq.asJava

  implicit def consumer[A](fun: A => Unit): Consumer[A] = new Consumer[A] {
    override def accept(t: A): Unit = fun(t)
  }

  /*    JavaFX Support    */
  implicit def callback[X, Y](fun: X => Y): Callback[X, Y] = new Callback[X, Y] {
    override def call(p: X): Y = fun(p)
  }

  implicit def eventHandler[A <: javafx.event.Event](fun: A => Unit): EventHandler[A] = new EventHandler[A] {
    override def handle(t: A): Unit = fun(t)
  }

  /*    RethinkDB support    */
  implicit def reqlFunction0(fun: () => Object): ReqlFunction0 = new ReqlFunction0 {
    override def apply(): Object = fun()
  }

  implicit def reqlFunction1(fun: (ReqlExpr) => Object): ReqlFunction1 = new ReqlFunction1 {
    override def apply(arg1: ReqlExpr): Object = fun(arg1)
  }

  implicit def reqlFunction2(fun: (ReqlExpr, ReqlExpr) => Object): ReqlFunction2 = new ReqlFunction2 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr): Object = fun(arg1, arg2)
  }

  implicit def reqlFunction3(fun: (ReqlExpr, ReqlExpr, ReqlExpr) => Object): ReqlFunction3 = new ReqlFunction3 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr, arg3: ReqlExpr): Object = fun(arg1, arg2, arg3)
  }

  implicit def reqlFunction4(fun: (ReqlExpr, ReqlExpr, ReqlExpr, ReqlExpr) => Object): ReqlFunction4 = new ReqlFunction4 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr, arg3: ReqlExpr, arg4: ReqlExpr): Object = fun(arg1, arg2, arg3, arg4)
  }

  /*    Akka    */
  implicit def actorSystem(context: ActorContext): ActorSystem = context.system


  /*    Others    */
  implicit def unit(x: Any): Unit = Unit

  implicit def not[A](f: A => Boolean)(a: A): Boolean = !f(a)

  /*  range  */
  def isWrapped(outer: immutable.Range, inner: immutable.Range): Boolean = {
    outer.contains(inner.start) && outer.contains(inner.last)
  }

  def hasIntersect(r1: immutable.Range, r2: immutable.Range): Boolean =
    r1.contains(r2.start) || r1.contains(r2.last) ||
      r2.contains(r1.start) || r2.contains(r1.last)

  def hasIntersect(r: immutable.Range): (immutable.Range) => Boolean = hasIntersect(r, _)

  @tailrec
  def nonIntersectRanges(r1: immutable.Range, r2: immutable.Range): Set[immutable.Range] = {
    if (hasIntersect(r1, r2)) {
      if (isWrapped(r1, r2))
        Set[immutable.Range](immutable.Range(r1.start, r2.start - r1.step, r1.step), immutable.Range(r2.last + r1.step, r2.last))
      else
        nonIntersectRanges(r2, r1)
    } else Set(r1, r2)
  }

  def nonIntersectRanges(r: immutable.Range): (immutable.Range) => Set[immutable.Range] = nonIntersectRanges(r, _)

  def remove(range: immutable.Range)(implicit ranges: mutable.Set[immutable.Range]) = {
    if (ranges.contains(range))
      ranges -= range
    else {
      val intersectedRanges = ranges.filter(hasIntersect(range))
      ranges.retain(not(intersectedRanges.contains))
      val remainedRanges = intersectedRanges.flatMap(nonIntersectRanges(range))
      ranges ++= remainedRanges
    }
  }

  implicit def fork(runnable: Runnable): Thread = {
    val thread = new Thread(runnable)
    thread.start()
    thread
  }
}
