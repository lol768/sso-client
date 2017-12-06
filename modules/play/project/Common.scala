
import scala.xml.XML

object Common {
  def projectVersion: String = {
    val xml = XML.loadFile("../../pom.xml")
    val version: String = (xml \ "version").text
    if(version == null || version.isEmpty) {
      throw new RuntimeException("Failed to load version from POM")
    }
    version
  }
}
