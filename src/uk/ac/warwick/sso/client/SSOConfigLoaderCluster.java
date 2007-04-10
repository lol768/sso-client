/*
 * Created on 07-Dec-2005
 *
 */
package uk.ac.warwick.sso.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.cache.DatabaseUserCache;
import uk.ac.warwick.sso.client.cache.UserCache;

/**
 * If using the clustered SSO mode you'll need a datasource called: java:/SSOUserCacheDS This must contain the following
 * table:
 * 
 * CREATE TABLE "OBJECTCACHE" ( "KEY" VARCHAR2(255 BYTE), "OBJECTDATA" BLOB, "CREATEDDATE" DATE, PRIMARY KEY ("KEY")
 * ENABLE ) ;
 * 
 * @author Kieran Shaw
 * @deprecated
 */
public class SSOConfigLoaderCluster extends SSOConfigLoader {

	private static final Logger LOGGER = Logger.getLogger(SSOConfigLoaderCluster.class);

	protected UserCache getCache() {

		LOGGER.info("Loading clustered DatabaseUserCache and InMemeoryUserCache");

		DatabaseUserCache dbCache = new DatabaseUserCache();
		dbCache.setDataSource(getDataSource());

		//UserCache memCache = new InMemoryUserCache();

		//TwoLevelUserCache twoLevelCache = new TwoLevelUserCache(memCache, dbCache);

		return dbCache;
	}

	private DataSource getDataSource() {
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
