package warwick.sso

import java.math.BigInteger
import java.{lang, math, util}
import java.util.{Collections, Properties}

import com.typesafe.config.{ConfigList, ConfigObject}

import scala.collection.JavaConverters._
import org.apache.commons.configuration.{Configuration => ApacheConfiguration}
import play.api.Configuration

import scala.util.Try


/**
 * Adapter for Play configuration to the Apache Commons Configuration interface.
 */
class PlayConfiguration(conf: Configuration) extends ApacheConfiguration {

  private def getConfigList(key: String): Option[Seq[Configuration]] =
    if (conf.has(key)) Some(conf.underlying.getConfigList(key).asScala.map(Configuration(_)))
    else None

  private def getStringList(key: String): Option[util.List[String]] =
    if (conf.has(key)) Some(conf.underlying.getStringList(key))
    else None

  private def getObjectList(key: String): Option[util.List[_ <: ConfigObject]] =
    if (conf.has(key)) Some(conf.underlying.getObjectList(key))
    else None

  /**
    * Subitems contained inside list items don't appear in config.keys, but Apache Configuration
    * expects them to return true for containsKey, so let's gather up all sub properties
    * of list members.
    */
  private lazy val listSubkeys: Set[String] =
    for {
      key <- conf.keys
      subConfs <- Try(getConfigList(key)).toOption.flatten.toSeq
      subConf <- subConfs
      item <- subConf.keys
    } yield s"$key.$item"

  private def noThanksWeAreImmutable: Nothing = throw new UnsupportedOperationException("Immutable config")

  override def addProperty(s: String, o: scala.Any): Unit = noThanksWeAreImmutable
  override def setProperty(s: String, o: scala.Any): Unit = noThanksWeAreImmutable
  override def clear(): Unit = noThanksWeAreImmutable
  override def clearProperty(s: String): Unit = noThanksWeAreImmutable

  override def getList(s: String): util.List[AnyRef] = getList(s, Collections.EMPTY_LIST)

  /**
    * First we see if config at `s` is a list itself. If it isn't...
    * Because Apache configuration works by flattening everything, you can get a list
    * of sub-items that appear within a sequence of list items, e.g. if app.providerid
    * is repeated in XML, you can getList("app.providerid") even though "app" is the list.
    * We implement that here by finding the first prefix that's a list, and gathering subitems
    * from each member.
    */
  override def getList(s: String, fallback: util.List[_]): util.List[AnyRef] =
    (getStringList(s) orElse {
      // any part of the key could be an array... we have to find it, then dig through each item in it.
      findListKey(s).flatMap { listKey =>
        val childKey = s.substring(listKey.length + 1)
        // this shouldn't happen as the initial getStringList should work.
        require(childKey.nonEmpty, "No child key to select")
        getObjectList(listKey).map { list =>
          list.asScala.map { obj =>
            obj.get(childKey).unwrapped()
          }.asJava
        }
      }
    } getOrElse {
      fallback
    }).asInstanceOf[util.List[AnyRef]]

  /** From a dot.separated.key, finds the first prefix that is a list. */
  def findListKey(key: String): Option[String] = {
    val parts = key.split("\\.")
    Stream.from(1).take(parts.length) map { n =>
      parts.take(n).mkString(".")
    } find { key =>
      Try(conf.getOptional[ConfigList](key)).isSuccess
    }
  }

  override def getProperty(s: String): AnyRef =
    if (conf.entrySet.exists { case (k, _) => k == s })
      conf.entrySet
        .find { case (k, _) => k == s }
        .map { case (_, v) => v.unwrapped }
        .orNull
    else
      Some(getList(s)).filterNot(_.isEmpty).orNull

  override def getKeys: util.Iterator[String] = conf.keys.iterator.asJava
  override def getKeys(prefix: String): util.Iterator[String] = conf.get[Configuration](prefix).keys.iterator.asJava

  override def subset(s: String): ApacheConfiguration = new PlayConfiguration(conf.get[Configuration](s))

  override def isEmpty: Boolean = conf.keys.isEmpty

  override def getProperties(s: String): Properties = {
    val p = new Properties()
    for ((key, value) <- conf.entrySet) p.setProperty(key, value.render)
    p
  }

  override def getDouble(s: String): Double = conf.get[Double](s)
  override def getDouble(s: String, v: Double): Double = conf.getOptional[Double](s).getOrElse(v)
  override def getDouble(s: String, v: lang.Double): lang.Double = getDouble(s, v)

  override def getFloat(s: String): Float = getDouble(s).toFloat
  override def getFloat(s: String, v: Float): Float = getDouble(s,v).toFloat
  override def getFloat(s: String, v: lang.Float): lang.Float = getDouble(s, v.toDouble).toFloat

  override def getBigDecimal(s: String): math.BigDecimal =
    new math.BigDecimal(conf.get[Number](s).toString)
  override def getBigDecimal(s: String, default: math.BigDecimal): math.BigDecimal =
    conf.getOptional[Number](s).map(n => new math.BigDecimal(n.toString)).getOrElse(default)

  override def getLong(s: String): Long = conf.get[Long](s)
  override def getLong(s: String, l: Long): Long = conf.getOptional[Long](s).getOrElse(l)
  override def getLong(s: String, aLong: lang.Long): lang.Long = getLong(s, aLong)

  override def getByte(s: String): Byte = conf.underlying.getBytes(s).toByte
  override def getByte(s: String, b: Byte): Byte =
    if (conf.has(s)) getByte(s)
    else b
  override def getByte(s: String, b: lang.Byte): lang.Byte = getByte(s,b)

  override def getBoolean(s: String): Boolean = conf.get[Boolean](s)
  override def getBoolean(s: String, b: Boolean): Boolean = conf.getOptional[Boolean](s).getOrElse(b)
  override def getBoolean(s: String, b: lang.Boolean): lang.Boolean = getBoolean(s,b)

  override def containsKey(s: String): Boolean =
    conf.keys.contains(s) || listSubkeys.contains(s)

  override def getShort(s: String): Short = getInt(s).toShort
  override def getShort(s: String, i: Short): Short = getInt(s,i.toInt).toShort
  override def getShort(s: String, i: lang.Short): lang.Short = getShort(s, i)

  override def getStringArray(s: String): Array[String] = getList(s).toArray(Array[String]())

  override def getBigInteger(s: String): BigInteger =
    new BigInteger(conf.get[Number](s).toString)
  override def getBigInteger(s: String, default: BigInteger): BigInteger =
    conf.getOptional[Number](s).map(n => new BigInteger(n.toString)).getOrElse(default)

  override def getInteger(s: String, integer: Integer): Integer = getInt(s, integer)

  override def getInt(s: String): Int = conf.get[Int](s)
  override def getInt(s: String, i: Int): Int = conf.getOptional[Int](s).getOrElse(i)

  override def getString(s: String): String = conf.get[String](s)
  override def getString(s: String, s1: String): String = conf.getOptional[String](s).getOrElse(s1)
}

