package hk.edu.polyu.datamining.pamap2.math

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