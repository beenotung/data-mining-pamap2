package hk.edu.polyu.datamining.pamap2.math

/**
  * Created by beenotung on 1/31/16.
  */
object Classify {
  def categoryMotion(motion: DisplacementVector): MotionCategory = {
    var front = false
    var back = false
    var left = false
    var right = false
    var up = false
    var down = false

    if (motion.x > 0) {
      front = true
    } else if (motion.x < 0) {
      back = true
    }
    if (motion.y > 0) {
      left = true
    } else if (motion.y > 0) {
      right = true
    }
    if (motion.z > 0) {
      up = true
    } else if (motion.z < 0) {
      down = false
    }

    new MotionCategory(up = up, down = down, left = left, right = right, front = front, back = back)
  }
}
