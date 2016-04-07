package hk.edu.polyu.datamining.pamap2.ui

import javax.swing.JFrame

import smile.plot.Hexmap

/**
  * Created by janus on 8/4/2016.
  */
class GraphFactory {
  /**
    * HexMapForSOM
    * Generate HexMap for SOM
    *
    * @param labels e.g [[<table border="1"><tr><td>Total</td><td align="right">42</td></tr><tr><td>class 0</td><td align="right">2.4%</td></tr><tr><td>class 6</td><td align="right">95.2%</td></tr><tr><td>class 7</td><td align="right">2.4%</td></tr></table>, ...]]
    * @param data e.g. [[1.7605168240755225, 2.265691955364412,...]]
    * @return
    */
  def HexMapForSOM(labels: Array[Array[String]], data: Array[Array[Double]])={
    val frame: JFrame = new JFrame("Hexmap")
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setLocationRelativeTo(null)
    frame.add(Hexmap.plot(labels, data))
  }

}
