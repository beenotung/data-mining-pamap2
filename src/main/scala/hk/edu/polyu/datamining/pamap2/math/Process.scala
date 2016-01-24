package hk.edu.polyu.datamining.pamap2.math

/**
  * Created by beenotung on 1/21/16.
  */

object Process {
  //TODO
  def findRelativeBodyMotion(arm: DisplacementPolar, hand: DisplacementPolar, ankle: DisplacementPolar): DisplacementPolar = ???

  //TODO
  def findRelativeBodyRotation(arm: EulerAngle, hand: EulerAngle, ankle: EulerAngle): EulerAngle = ???

  //TODO
  def rectReduce(rect16: DisplacementVector, rect6: DisplacementVector): DisplacementVector = ???

  //TODO
  def rectToPolar(rect: DisplacementVector, magnet: DisplacementVector): DisplacementPolar = ???

  //TODO
  def rotationVectorToEulerAngle(rotationVector: RotationVector): EulerAngle = ???

  /**
    * treat average/chest as 0
    * return relative Thermodynamic temperature of each body part
    *
    * @param arm   : arm absolute temperature
    * @param chest : chest absolute temperature
    * @param ankle : ankle absolute temperature
    * @return (arm,chest,ankle) : relative temperature
    *         -1.0..1.0
    **/
  //TODO
  def findRelativeTemperature(arm: Float, chest: Float, ankle: Float): (Float, Float, Float) = ???
}
