package warwick.sso

import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.db.DBApi
import play.api.test.FakeApplication
import play.api.test._
import play.api.test.Helpers._
import uk.ac.warwick.sso.client.{SSOToken, SSOConfiguration}
import uk.ac.warwick.sso.client.cache.{UserCacheItem, UserCache}

class JdbcUserCacheSpec extends PlaySpec with MockitoSugar {

  "JdbcUserCache" should {

    "store blobs correctly" in  {
      val app = FakeApplication(additionalConfiguration = Map() ++ inMemoryDatabase())
      val config = app.injector.instanceOf[Configuration]
      val ssoConfig = new SSOConfiguration(new PlayConfiguration(config))
      val db = app.injector.instanceOf[DBApi].database("default")
      val delegate = mock[UserCache]

      db.withConnection { conn =>
        conn.createStatement().execute("""
        |CREATE TABLE objectcache (
        |    "KEY" nvarchar2 (100) NOT NULL,
        |    "OBJECTDATA" BLOB,
        |    "CREATEDDATE" TIMESTAMP (6),
        |    CONSTRAINT "OBJECTCACHE_PK" PRIMARY KEY ("KEY")
        |);
        """.stripMargin)
      }

      val userCache = new JdbcUserCache(ssoConfig, db, delegate)

      val token = new SSOToken("123", SSOToken.SSC_TICKET_TYPE)
      val item = new UserCacheItem(
        new uk.ac.warwick.userlookup.User("x"),
        new DateTime().getMillis,
        token
      )
      userCache.put(token, item)

      val result = userCache.get(token)
      result.getToken() must be (token)
      result.getUser().getUserId must be ("x")
    }

  }

}
