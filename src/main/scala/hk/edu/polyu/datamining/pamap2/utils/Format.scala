package hk.edu.polyu.datamining.pamap2.utils

/**
  * Created by beenotung on 1/31/16.
  */
object Format {
  def toFloat(string: String): Float = try {
    string.toFloat
  } catch {
    case e: NumberFormatException => Float.NaN
  }

  def toDouble(string: String): Double = try {
    string.toDouble
  } catch {
    case e: NumberFormatException => Double.NaN
  }

  def toShort(string: String): Short = try {
    string.toShort
  } catch {
    case e: NumberFormatException => Short.MinValue
  }

  def toByte(string: String): Byte = try {
    string.toByte
  } catch {
    case e: NumberFormatException => Byte.MinValue
  }
}
