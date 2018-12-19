import sbt._
import scala.sys.process._

object Cucumber {
  def run(classPath: Seq[File]) = {
    val value = "java -cp \"" + classPath.map(_.getAbsolutePath).mkString(":") + "\" org.jruby.Main -h"
    println(value)
    Process (value) !
  }


}
