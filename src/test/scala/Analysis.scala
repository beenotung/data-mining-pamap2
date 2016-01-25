import java.nio.file.{Files, Paths}

import scala.language.postfixOps

/**
  * Created by beenotung on 1/20/16.
  */
object Analysis extends App {
  val lines = Range.inclusive(1, 54).map(i => {
    s"\necho == $i ==" +
      s"\ncat subject101.dat  | awk '{print $$$i}' | grep -v NaN | sort -n | uniq > head ; cat head | head -n 1 ; cat head | tail -n 1" +
      s"\ncat subject101.dat  | awk '{print $$$i}' | grep NaN | uniq"
  })
  val filename = "analysis.sh"
  Files.write(Paths.get(filename), lines.fold("")(_ + "\n" + _).getBytes)
}
