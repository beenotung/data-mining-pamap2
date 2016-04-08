import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}

import scala.util.Random

/**
  * Created by beenotung on 4/9/16.
  */
object SomTest2 extends App {
  val size = 100
  val som = new Som(Array(1, 1, 1), size, size, "hand_x") {
    override def randomInit: () => Vector = () => Array.tabulate(3)(_ => Math.random())
  }
  var notOk = true
  do {
    val res = som.addSample(SomTest.dataset(Random.nextInt(SomTest.dataset.length)))
    println(s"error:$res")
    notOk = res > 0.001
  } while (notOk)
  val (label, distance) = som.getLabel(SomTest.dataset(0))
  println(s"c:$label\ndistance:$distance\nt:${som.t}")
}
