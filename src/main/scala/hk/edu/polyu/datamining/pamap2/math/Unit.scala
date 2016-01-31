package hk.edu.polyu.datamining.pamap2.math

import scala.collection.JavaConverters._

/**
  * Created by beenotung on 1/21/16.
  */

class DisplacementVector(val x: Float, val y: Float, val z: Float)

class DisplacementPolar(val radius: Float, val theta: Float, val phi: Float)

/** reference :
  * https://en.wikipedia.org/wiki/Flight_dynamics
  * https://www.youtube.com/watch?v=q0jgqeS_ACM
  * */
class RotationVector(val yaw: Float, val pitch: Float, val roll: Float)

/** reference : https://en.wikipedia.org/wiki/Euler_angles */
class EulerAngle(val radius: Float, val alpha: Float, val beta: Float, val gamma: Float)

class MotionCategory(val up: Boolean, val down: Boolean, val left: Boolean, val right: Boolean, val front: Boolean, val back: Boolean)

object MotionCategory extends Enumeration {
  type MotionType = Value
  val up, down, left, right, front, back = Value

  def toList(motion: MotionCategory): java.util.List[String] =
    toMap(motion)
      .filter(_._2)
      .keys.toList.asJava

  def toMap(motion: MotionCategory): Map[String, Boolean] =
    Map(
      (up.toString, motion.up),
      (down.toString, motion.down),
      (left.toString, motion.left),
      (right.toString, motion.right),
      (front.toString, motion.front),
      (back.toString, motion.back)
    )

  def fromList(flags: java.util.List[String]): MotionCategory =
    new MotionCategory(
      up = flags.contains(up.toString),
      down = flags.contains(down.toString),
      left = flags.contains(left.toString),
      right = flags.contains(right.toString),
      front = flags.contains(front.toString),
      back = flags.contains(back.toString)
    )

  def fromMap(map: Map[String, Boolean]): MotionCategory =
    new MotionCategory(
      up = map.getOrElse(up.toString, false),
      down = map.getOrElse(down.toString, false),
      left = map.getOrElse(left.toString, false),
      right = map.getOrElse(right.toString, false),
      front = map.getOrElse(front.toString, false),
      back = map.getOrElse(back.toString, false)
    )
}