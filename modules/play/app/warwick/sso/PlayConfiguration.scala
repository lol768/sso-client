package warwick.sso

import java.math.BigInteger
import java.{lang, math, util}
import java.util.{Collections, Properties}

import scala.collection.JavaConverters._

import org.apache.commons.configuration.{Configuration => ApacheConfiguration, AbstractConfiguration}
import play.api.Configuration


/**
 * Adapter for Play configuration to the Apache Commons Configuration interface.
 */
class PlayConfiguration(conf: Configuration) extends ApacheConfiguration {

  private def noThanksWeAreImmutable: Nothing = throw new UnsupportedOperationException("Immutable config")

  override def addProperty(s: String, o: scala.Any): Unit = noThanksWeAreImmutable
  override def setProperty(s: String, o: scala.Any): Unit = noThanksWeAreImmutable
  override def clear(): Unit = noThanksWeAreImmutable
  override def clearProperty(s: String): Unit = noThanksWeAreImmutable

  override def getList(s: String): util.List[_] = getList(s, Collections.EMPTY_LIST)
  override def getList(s: String, orElse: util.List[_]): util.List[_] = conf.getStringList(s).getOrElse(orElse)

  override def getProperty(s: String): AnyRef =
    conf.entrySet
      .find { case (k, v) => k == s }
      .map { case (k, v) => v.unwrapped }
      .orNull

  override def getKeys: util.Iterator[String] = conf.keys.iterator.asJava
  override def getKeys(prefix: String): util.Iterator[String] = conf.getConfig(prefix).get.keys.iterator.asJava

  override def subset(s: String): ApacheConfiguration = new PlayConfiguration(conf.getConfig(s).get)

  override def isEmpty: Boolean = conf.keys.isEmpty

  override def getProperties(s: String): Properties = {
    val p = new Properties()
    for ((key, value) <- conf.entrySet) p.setProperty(key, value.render)
    p
  }

  override def getDouble(s: String): Double = conf.getDouble(s).get
  override def getDouble(s: String, v: Double): Double = conf.getDouble(s).getOrElse(v)
  override def getDouble(s: String, v: lang.Double): lang.Double = getDouble(s, v)

  override def getFloat(s: String): Float = getDouble(s).toFloat
  override def getFloat(s: String, v: Float): Float = getDouble(s,v).toFloat
  override def getFloat(s: String, v: lang.Float): lang.Float = getDouble(s, v.toDouble).toFloat

  override def getBigDecimal(s: String): math.BigDecimal =
    conf.getNumber(s).map(n => new math.BigDecimal(n.toString)).get
  override def getBigDecimal(s: String, default: math.BigDecimal): math.BigDecimal =
    conf.getNumber(s).map(n => new math.BigDecimal(n.toString)).getOrElse(default)

  override def getLong(s: String): Long = conf.getLong(s).get
  override def getLong(s: String, l: Long): Long = conf.getLong(s).getOrElse(l)
  override def getLong(s: String, aLong: lang.Long): lang.Long = getLong(s, aLong)

  override def getByte(s: String): Byte = conf.getBytes(s).get.toByte
  override def getByte(s: String, b: Byte): Byte = conf.getBytes(s).map(_.toByte).getOrElse(b)
  override def getByte(s: String, b: lang.Byte): lang.Byte = getByte(s,b)

  override def getBoolean(s: String): Boolean = conf.getBoolean(s).get
  override def getBoolean(s: String, b: Boolean): Boolean = conf.getBoolean(s).getOrElse(b)
  override def getBoolean(s: String, b: lang.Boolean): lang.Boolean = getBoolean(s,b)

  override def containsKey(s: String): Boolean = conf.keys(s)

  override def getShort(s: String): Short = getInt(s).toShort
  override def getShort(s: String, i: Short): Short = getInt(s,i.toInt).toShort
  override def getShort(s: String, i: lang.Short): lang.Short = getShort(s, i)

  override def getStringArray(s: String): Array[String] = getList(s).toArray(Array[String]())

  override def getBigInteger(s: String): BigInteger =
    conf.getNumber(s).map(n => new BigInteger(n.toString)).get
  override def getBigInteger(s: String, bigInteger: BigInteger): BigInteger =
    conf.getNumber(s).map(n => new BigInteger(n.toString)).getOrElse(bigInteger)

  override def getInteger(s: String, integer: Integer): Integer = getInt(s, integer)


  override def getInt(s: String): Int = conf.getInt(s).get
  override def getInt(s: String, i: Int): Int = conf.getInt(s).getOrElse(i)

  override def getString(s: String): String = conf.getString(s).get
  override def getString(s: String, s1: String): String = conf.getString(s).getOrElse(s1)
}

