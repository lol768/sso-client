package warwick.sso

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.{Date, ResultSet}
import java.time.Instant

import javax.inject.{Inject, Named}
import play.api.Logger
import play.api.db._
import uk.ac.warwick.sso.client.cache.{UserCache, UserCacheItem}
import uk.ac.warwick.sso.client.{SSOConfiguration, SSOToken}

/**
 * Implementation of UserCache that saves to the database.
 * It is used when cluster.enabled is true.
 *
 * It is analogous to DatabaseUserCache from the origin SSOClient,
 * which used Spring JDBC support to do almost exactly the same stuff.
 * This just uses Play's JDBC support instead and is enabled for
 * dependency injection.
 */
class JdbcUserCache @Inject() (
     config: SSOConfiguration,
     @Named("SSOClientDB") db: Database,
     @Named("InMemory") delegate: UserCache
  ) extends UserCache {

  var databaseEnabled: Boolean = true

  private val logger = Logger(getClass)
  private val timeout: Int = config.getInt("ssoclient.sessioncache.database.timeout.secs")

  override def put(key: SSOToken, value: UserCacheItem): Unit =
    if (databaseEnabled) {
      remove(key)
      db.withConnection { conn =>
        val stmt = conn.prepareStatement("insert into objectcache (key, objectdata, createddate) values (?,?,?)")

        val baos = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(baos)
        oos.writeObject(value)
        oos.close()

        val bytes = baos.toByteArray

        stmt.setString(1, key.getValue)
        stmt.setBinaryStream(2, new ByteArrayInputStream(bytes), bytes.length)
        stmt.setDate(3, new Date(Instant.now.toEpochMilli))

        stmt.executeUpdate()
      }
    } else {
      delegate.put(key, value)
    }


  override def remove(ssoToken: SSOToken): Unit = {
    if (databaseEnabled) {
      db.withConnection { conn =>
        val stmt = conn.prepareStatement("delete from objectcache where key = ?")
        stmt.setString(1, ssoToken.getValue)
        stmt.execute()
      }
    } else {
      delegate.remove(ssoToken)
    }
  }

  override def get(ssoToken: SSOToken): UserCacheItem =
    if (databaseEnabled) {
      db.withConnection { implicit conn =>
        val stmt = conn.prepareStatement("select objectdata from objectcache where key = ?")
        stmt.setString(1, ssoToken.getValue)
        val results = stmt.executeQuery()
        val item: Option[UserCacheItem] = readItem(results)

        item match {
          case None => null
          case Some(i) if expired(i) => remove(ssoToken); null
          case Some(i) => i
        }
      }
    } else {
      delegate.get(ssoToken)
    }

  private def readItem(results: ResultSet): Option[UserCacheItem] =
    Option(results).filter(_.next())
      .flatMap { r => Option(r.getBinaryStream("objectdata")) }
      .flatMap { is =>
        try {
          Option(new ObjectInputStream(is).readObject().asInstanceOf[UserCacheItem])
        } catch {
          case e: Exception =>
            logger.error("Could not get cache item back from database", e)
            None
        }
      }

  private def expired(it: UserCacheItem) = Instant.ofEpochMilli(it.getInTime).plusSeconds(timeout).isBefore(Instant.now)
}
