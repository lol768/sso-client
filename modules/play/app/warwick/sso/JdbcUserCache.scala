package warwick.sso

import java.io.{ObjectOutputStream, IOException, ObjectInputStream}
import java.sql.{Date, Blob, ResultSet}
import javax.inject.{Named, Inject}

import org.joda.time.DateTime
import play.api.Logger
import play.api.db._

import uk.ac.warwick.sso.client.{SSOConfiguration, SSOToken}
import uk.ac.warwick.sso.client.cache.{UserCacheItem, UserCache}

import scala.util.Try

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
        val blob = conn.createBlob()
        val output = blob.setBinaryStream(1)
        val oos = new ObjectOutputStream(output)
        oos.writeObject(value)
        oos.close()
        stmt.setString(1, key.getValue)
        stmt.setBlob(2, blob)
        stmt.setDate(3, new Date(new DateTime().getMillis))

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
          case Some(item) if expired(item) => remove(ssoToken); null
          case Some(item) => item
        }
      }
    } else {
      delegate.get(ssoToken)
    }

  private def readItem(results: ResultSet): Option[UserCacheItem] =
    readBlob(results, "objectdata").flatMap { blob =>
      try {
        Option(new ObjectInputStream(blob.getBinaryStream).readObject().asInstanceOf[UserCacheItem])
      } catch {
        case e @ (_:IOException | _:ClassNotFoundException) =>
          logger.error("Could not get cache item back from database", e)
          None
      }
    }

  private def readBlob(results: ResultSet, name: String) =
  if (results.next()) Option(results.getBlob(name))
  else None

  private def expired(it: UserCacheItem) = new DateTime(it.getInTime).plusSeconds(timeout).isBeforeNow
}
