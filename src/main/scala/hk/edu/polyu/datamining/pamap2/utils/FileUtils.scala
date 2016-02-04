package hk.edu.polyu.datamining.pamap2.utils

import java.io.File

import scala.io.Source

/**
  * Created by beenotung on 2/4/16.
  */
object FileUtils {
  def lineCount(file: File) = Source.fromFile(file).getLines().length
}
