package hk.edu.polyu.datamining.pamap2.utils

import com.rethinkdb.gen.ast._

import scala.language.implicitConversions

/**
  * Created by beenotung on 1/25/16.
  */
object Lang {
  implicit def runnable(fun: () => Unit): Runnable = new Runnable {
    override def run() = fun
  }

  implicit def reqlFunction0(fun: () => Object): ReqlFunction0 = new ReqlFunction0 {
    override def apply(): Object = fun
  }

  implicit def reqlFunction1(fun: (ReqlExpr) => Object): ReqlFunction1 = new ReqlFunction1 {
    override def apply(arg1: ReqlExpr): AnyRef = fun
  }

  implicit def reqlFunction2(fun: (ReqlExpr, ReqlExpr) => Object): ReqlFunction2 = new ReqlFunction2 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr): AnyRef = fun
  }

  implicit def reqlFunction3(fun: () => Object): ReqlFunction3 = new ReqlFunction3 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr, arg3: ReqlExpr): AnyRef = fun
  }

  implicit def reqlFunction4(fun: () => Object): ReqlFunction4 = new ReqlFunction4 {
    override def apply(arg1: ReqlExpr, arg2: ReqlExpr, arg3: ReqlExpr, arg4: ReqlExpr): AnyRef = fun
  }
}
