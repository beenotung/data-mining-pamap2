package hk.edu.polyu.datamining.pamap2.math

/**
  * Created by beenotung on 1/21/16.
  */

import scala.math.{Pi, atan, pow, sqrt, toDegrees}

object Process {

  def vectorToPolar(part: DisplacementVector): DisplacementPolar = {
    val radius: Double = sqrt(pow(part.x, 2) + pow(part.y, 2) + pow(part.z, 2))

    // theta
    val theta: Double =
      if (part.x > 0) {
        toDegrees(atan(part.y / part.x))
      } else if (part.x < 0 && part.y > 0) {
        toDegrees(atan(part.y / part.x)) + Pi
      } else if (part.x < 0 && part.y < 0) {
        toDegrees(atan(part.y / part.x)) - Pi
      } else if (part.x == 0 && part.y > 0) {
        Pi / 2
      } else {
        //x==0 && y<0
        Pi / (-2)
      }


    //phi
    val phi: Double =
      if (part.z > 0) {
        toDegrees(atan(sqrt(pow(part.x, 2) + pow(part.y, 2)) / part.z))
      } else if (part.z < 0) {
        toDegrees(atan(sqrt(pow(part.x, 2) + pow(part.y, 2)) / part.z)) + Pi
      } else {
        // Z==0
        Pi / 2
      }

    new DisplacementPolar(radius.toFloat, theta.toFloat, phi.toFloat)
  }

  def motionToBoolean(part: DisplacementVector): (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) = {
    var front = false
    var back = false
    var left = false
    var right = false
    var up = false
    var down = false

    if (part.x > 0) {
      front = true
    } else if (part.x < 0) {
      back = true
    }
    if (part.y > 0) {
      left = true
    } else if (part.y > 0) {
      right = true
    }
    if (part.z > 0) {
      up = true
    } else if (part.z < 0) {
      down = false
    }
    (front, back, left, right, up, down)
  }

  //TODO
  def findRelativeBodyMotion(chest: DisplacementPolar, hand: DisplacementPolar, ankle: DisplacementPolar): DisplacementPolar = ???

  //TODO
  def findRelativeBodyRotation(chest: EulerAngle, hand: EulerAngle, ankle: EulerAngle): EulerAngle = ???

  //TODO
  def rectReduce(rect16: DisplacementVector, rect6: DisplacementVector): DisplacementVector = ???

  //TODO
  def rectToPolar(rect: DisplacementVector, magnet: DisplacementVector): DisplacementPolar = ???

  //TODO
  def rotationVectorToEulerAngle(rotationVector: RotationVector): EulerAngle = ???

  /**
    * treat chest as 1
    * return relative Thermodynamic temperature of each body part
    *
    * @param arm   : arm absolute temperature
    * @param chest : chest absolute temperature
    * @param ankle : ankle absolute temperature
    * @return (arm,chest,ankle) : relative temperature
    *         -1.0..1.0
    **/
  def findRelativeTemperature(arm: Float, chest: Float, ankle: Float): (Float, Float, Float) = {
    val arm_k = arm + 273
    val chest_k = chest + 273
    val ankle_k = ankle + 273
    (arm_k / chest_k, 1, ankle_k / chest_k)
  }

  def categoryMotion(motion: DisplacementPolar): MotionCategory = ???
}
