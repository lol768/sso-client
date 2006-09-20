/*
 * Created on 07-Dec-2005
 *
 */
package uk.ac.warwick.sso.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import uk.ac.warwick.sso.client.cache.DatabaseUserCache;
import uk.ac.warwick.sso.client.cache.InMemoryUserCache;
import uk.ac.warwick.sso.client.cache.TwoLevelUserCache;
import uk.ac.warwick.sso.client.cache.UserCache;

public class SSOConfigLoaderCluster extends SSOConfigLoader {

	protected static UserCache getCache() {

		DatabaseUserCache dbCache = new DatabaseUserCache();
		dbCache.setDataSource(getDataSource());

		UserCache memCache = new InMemoryUserCache();

		TwoLevelUserCache twoLevelCache = new TwoLevelUserCache(memCache, dbCache);

		return twoLevelCache;
	}

	private static DataSource getDataSource() {
		InitialContext ctx;
		DataSource ds = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:/SSOUserCacheDS");
		} catch (NamingException e) {
			throw new RuntimeException("Could not find datasource under key java:/SSOUserCacheDS", e);
		}
		return ds;
	}

}
