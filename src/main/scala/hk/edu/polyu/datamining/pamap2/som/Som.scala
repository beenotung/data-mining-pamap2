package hk.edu.polyu.datamining.pamap2.som

import hk.edu.polyu.datamining.pamap2.som.Som._
import java.{util => ju}

import scala.collection.JavaConverters._
import com.rethinkdb.RethinkDB.r
import hk.edu.polyu.datamining.pamap2.database.Tables.SomImage._

import scala.language.postfixOps
import scala.util.Random

/**
  * Created by beenotung on 4/9/16.
  * based on SOM.java
  * 2D grid
  * reference : https://en.wikipedia.org/wiki/Self-organizing_map
  */
object Som {

  object NormType extends Enumeration {
    type Field = Value
    val none, sigmoid = Value
  }

  def sigmoid(x: Double) = 1d / (1d + Math.exp(-x))

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

  def fromMap(map: ju.Map[String, AnyRef]): Option[Som] = {
    try {
      Some(new Som(
        weights = map.get(Weights).asInstanceOf[ju.List[Double]].asScala.toArray,
        labelPrefix = map.get(LabelPrefix).asInstanceOf[String],
        initGrids = {
          val gridMaps = map.get(Grids).asInstanceOf[ju.List[ju.Map[String, AnyRef]]]
          gridMaps.asScala.toSeq.map(gridMap => {
            val d1 = gridMap.get(D1).asInstanceOf[Int]
            val d2 = gridMap.get(D2).asInstanceOf[Int]
            val vector_s = gridMap.get(Vector_s).asInstanceOf[ju.List[Double]].asScala.toArray
            (d1, d2, vector_s)
          })
        }
      ))
    } catch {
      case e: Exception => None
    }
  }

  def randomGrids(nDimension: Int, gridWidth: Int, gridHeight: Int, min: Double, max: Double): Seq[Grid] =
    Seq.tabulate(gridWidth, gridHeight)((d1, d2) =>
      (d1,
        d2,
        Array.tabulate(nDimension)(_ => Random.nextDouble() * (max - min) + min))
    ).flatten
}

class Som(weights: Vector, val labelPrefix: String, initGrids: Seq[Grid], normMode: NormType.Field = NormType.none) {
  val grids: Seq[Grid] = {
    val max = initGrids.flatten(_._3).max
    initGrids.map(x => (x._1, x._2, x._3.map(_ / max)))
  }

  def toMap: com.rethinkdb.model.MapObject = {
    r.hashMap(Weights, weights.toList.asJava)
      .`with`(LabelPrefix, labelPrefix)
      .`with`(Grids, grids.map(x => r.hashMap()
        .`with`(D1, x._1)
        .`with`(D2, x._2)
        .`with`(Vector_s, x._3.toList.asJava)
      ).toList.asJava)
  }

  val nDimension = weights.length
  val normWeights: Vector = {
    val sum = weights.sum
    weights.map(_ / sum)
  }

  /* for performance */
  var alpha = Som.alpha0
  var thetaR = 1d

  var t = 0L

  def addSample(oriSample: Vector) = {
    val sample = normMode match {
      case NormType.sigmoid => oriSample.map(sigmoid)
      case _ => oriSample
    }
    t += 1
    val (closest, _) = findClosestGrid(sample)
    var change = 0d
    grids.foreach(grid => {
      val gridDistance = Som.gridDistance(closest, grid)
      normMode match {
        case NormType.sigmoid =>
          grid._3.indices.foreach(i => {
            val w = grid._3(i)
            val d = Math.pow(gridDistance, Som.distanceRatio) * thetaR * alpha * (sample(i) - w)
            grid._3(i) = w + d
            change += Math.abs(d)
          })
        case _ =>
          grid._3.indices.foreach(i => {
            val w = grid._3(i)
            val d = Math.pow(gridDistance, Som.distanceRatio) * thetaR * alpha * (sample(i) - w)
            grid._3(i) = w + d
            change += Math.abs(d)
          })
      }
    })
    alpha *= Som.alphaDecreaseRatio
    thetaR *= Som.distanceDecreaseRatio
    change
  }

  def findClosestGrid(a: Vector): (Grid, Double) =
    grids.map(grid => (grid, Som.similarMeasure(normWeights, a, grid._3)))
      .minBy(_._2)

  def getLabel(oriXs: Vector): (String, Double) = {
    val xs = normMode match {
      case NormType.sigmoid => oriXs.map(sigmoid)
      case _ => oriXs
    }
    val (grid, distance) = findClosestGrid(xs)
    (s"$labelPrefix:${grid._1},${grid._2}|", Math.sqrt(distance))
  }
}
