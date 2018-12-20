import sbt._
import scala.sys.process._

object Cucumber {
  def run(classPath: Seq[File]) = {
    val mainClass = "org.jruby.Main"
    val args = Seq("-I", "modules/servlet/cucumber/lib", "modules/servlet/cucumber/bin/cucumber", "--dry-run", "--format", "html", "--out", "target/feature-spec.html", "modules/servlet/src/test/features/")
    val fo = ForkOptions().withRunJVMOptions(Vector[String]("-cp", classPath.map(_.getAbsolutePath).mkString(":")))
    println( classPath.map(_.getAbsolutePath).mkString(":"))
    val process = sbt.Fork.java.fork(fo, args)
  }


}
