package hk.edu.polyu.datamining.pamap2.math

/**
  * Created by beenotung on 1/21/16.
  */

object Process {
  //TODO
  def findRelativeBodyMotion(arm16: Vector, arm6: Vector, chest16: Vector, chest6: Vector, ankle16: Vector, ankle6: Vector): Polar = ???

  //TODO
  def rectReduce(rect16: Vector, rect6: Vector): Vector = ???

  //TODO
  def rectToPolar(rect: Vector, magnet: Vector): Polar = ???

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
