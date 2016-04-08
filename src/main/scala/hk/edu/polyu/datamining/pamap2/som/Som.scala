package hk.edu.polyu.datamining.pamap2.som

import hk.edu.polyu.datamining.pamap2.som.Som.similarMeasure

/**
  * Created by beenotung on 4/9/16.
  * based on SOM.java
  * 2D grid
  * reference : https://en.wikipedia.org/wiki/Self-organizing_map
  */
object Som {
  val alpha0 = 0.8
  val alphaDecreaseRatio = 0.99
  val distanceRatio = 0.5
  val distanceDecreaseRatio = 0.9

  def gridDistance(a: Grid, b: Grid): Double = {
    Math.sqrt(
      Math.pow(a._1 - b._1, 2)
        + Math.pow(a._2 - b._2, 2)
    )
  }

  /* learning rate, become smaller than 0.01 when t excess 4379 */
  def alpha(t: Long): Double = alpha0 * Math.pow(alphaDecreaseRatio, t)

  /* restraint */
  def theta(gridDistance: Double, t: Long): Double = Math.pow(gridDistance, distanceRatio) * Math.pow(distanceDecreaseRatio, t)

  def similarMeasure(weights: Vector, a: Vector, b: Vector): Double = {
    //    Math.sqrt(
    weights.indices
      .map(i => a(i) - b(i))
      .map(x => x * x)
      .sum
    //    )
  }
}

abstract class Som(val weights: Vector, val gridWidth: Int, val gridHeight: Int, val labelPrefix: String) {
  val nDimension = weights.length

  /* for performance */
  var alpha = Som.alpha0
  var thetaR = 1d

  def randomInit: () => Vector

  val grids: Seq[Grid] = Seq.tabulate(gridWidth, gridHeight)((d1, d2) => (d1, d2, randomInit())).flatten


  var t = 0L

  def addSample(sample: Vector) = {
    t += 1
    val (closest, _) = findClosestGrid(sample)
    var change = 0d
    grids.foreach(grid => {
      val gridDistance = Som.gridDistance(closest, grid)
      grid._3.indices.foreach(i => {
        val w = grid._3(i)
        var d = Math.pow(gridDistance, Som.distanceRatio) * thetaR * alpha * (sample(i) - w)
        grid._3(i) = w + d
        change += Math.abs(d)
      }
      )
    })
    alpha *= Som.alphaDecreaseRatio
    thetaR *= Som.distanceDecreaseRatio
    change
  }

  def findClosestGrid(a: Vector): (Grid, Double) =
    grids.map(grid => (grid, Som.similarMeasure(weights, a, grid._3)))
      .minBy(_._2)

  def getLabel(a: Vector): (String, Double) = {
    val (grid, distance) = findClosestGrid(a)
    (s"$labelPrefix:${grid._1},${grid._2}", distance)
  }
}
