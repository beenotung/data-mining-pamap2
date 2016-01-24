package hk.edu.polyu.datamining.pamap2.math

/**
  * Created by beenotung on 1/21/16.
  */

class DisplacementVector(var x: Float, var y: Float, var z: Float)

class DisplacementPolar(var radius: Float, var theta: Float, var phi: Float)

/** reference :
  * https://en.wikipedia.org/wiki/Flight_dynamics
  * https://www.youtube.com/watch?v=q0jgqeS_ACM
  * */
class RotationVector(var yaw: Float, var pitch: Float, var roll: Float)

/** reference : https://en.wikipedia.org/wiki/Euler_angles */
class EulerAngle(var radius: Float, var alpha: Float, var beta: Float, var gamma: Float)