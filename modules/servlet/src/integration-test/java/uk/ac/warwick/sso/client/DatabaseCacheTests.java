/*
 * Created on 07-Dec-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.util.Date;

import junit.framework.TestCase;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import uk.ac.warwick.sso.client.cache.spring.DatabaseUserCache;
import uk.ac.warwick.sso.client.cache.InMemoryUserCache;
import uk.ac.warwick.sso.client.cache.TwoLevelUserCache;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.userlookup.User;

public class DatabaseCacheTests extends TestCase {

	public final void testDatabaseCacheGetNoResults() {

		DriverManagerDataSource dataSource = getDataSource();

		DatabaseUserCache cache = new DatabaseUserCache();
		cache.setDataSource(dataSource);

		SSOToken token = new SSOToken("NothingHere", SSOToken.SSC_TICKET_TYPE);

		assertNull(cache.get(token));

	}

	public final void testDatabaseCachePut() {

		DriverManagerDataSource dataSource = getDataSource();

		DatabaseUserCache cache = new DatabaseUserCache();
		cache.setDataSource(dataSource);

		SSOToken token = new SSOToken("123456", SSOToken.SSC_TICKET_TYPE);
		User user = new User();
		user.setUserId("test");
		UserCacheItem item = new UserCacheItem(user, new Date().getTime(), token);

		cache.put(token, item);

		item = cache.get(token);
		assertNotNull(item);

	}

	public final void testDatabaseCacheGet() {

		DriverManagerDataSource dataSource = getDataSource();

		DatabaseUserCache cache = new DatabaseUserCache();
		cache.setDataSource(dataSource);

		SSOToken token = new SSOToken("123456", SSOToken.SSC_TICKET_TYPE);

		UserCacheItem item = cache.get(token);
		assertNotNull(item);

	}

	public final void testDatabaseCacheRemove() {

		DriverManagerDataSource dataSource = getDataSource();

		DatabaseUserCache cache = new DatabaseUserCache();
		cache.setDataSource(dataSource);

		SSOToken token = new SSOToken("123456", SSOToken.SSC_TICKET_TYPE);

		cache.remove(token);

		UserCacheItem item = cache.get(token);
		assertNull(item);

	}

	public final void testTwoLevelCache() {

		DriverManagerDataSource dataSource = getDataSource();

		DatabaseUserCache dbCache = new DatabaseUserCache();
		dbCache.setDataSource(dataSource);

		UserCache memCache = new InMemoryUserCache();

		TwoLevelUserCache twoLevelCache = new TwoLevelUserCache(memCache, dbCache);

		SSOToken token = new SSOToken("123456", SSOToken.SSC_TICKET_TYPE);
		User user = new User();
		user.setUserId("test");
		UserCacheItem item = new UserCacheItem(user, new Date().getTime(), token);

		twoLevelCache.put(token, item);

		item = twoLevelCache.get(token);

		assertNotNull(item);

		memCache.remove(token);
		// should still be able to get out of db cache
		item = twoLevelCache.get(token);

		assertNotNull(item);

		// should now be pushed back into mem cache
		item = memCache.get(token);
		assertNotNull(item);

		twoLevelCache.remove(token);
		// now all gone
		item = twoLevelCache.get(token);
		assertNull(item);

	}

	/**
	 * @return
	 */
	private DriverManagerDataSource getDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
		dataSource.setUrl("jdbc:oracle:thin:@ginseng:1521:ginseng");
		dataSource.setUsername("sso_origin");
		dataSource.setPassword("r1ght_j4b");
		return dataSource;
	}

}
