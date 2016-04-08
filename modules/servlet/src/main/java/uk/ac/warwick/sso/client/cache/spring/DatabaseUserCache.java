/*
 * Created on 06-Dec-2005
 *
 */
package uk.ac.warwick.sso.client.cache.spring;

import static java.lang.Integer.*;
import static uk.ac.warwick.userlookup.UserLookup.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.SSOToken;
import uk.ac.warwick.sso.client.cache.InMemoryUserCache;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;

public class DatabaseUserCache implements UserCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUserCache.class);

	private final SSOConfiguration config;

	private DataSource _dataSource;

	private int _timeout;

	private String _keyName = "key";
	
	private boolean _databaseEnabled = true;
	
	// A delegate user cache to be used when a database is in read-only mode
	private final UserCache _delegate;
	
	public DatabaseUserCache(SSOConfiguration config) {
		this(config, new InMemoryUserCache(config));
	}
	
	public DatabaseUserCache(SSOConfiguration config, UserCache delegate) {
		this.config = config;
		this._delegate = delegate;
		this._timeout = config.getInt("ssoclient.sessioncache.database.timeout.secs");
	}

	public final UserCacheItem get(final SSOToken key) {
		if (!_databaseEnabled) {
			return _delegate.get(key);
		}

		LOGGER.debug("Getting item from database cache " + key.toString());

		JdbcTemplate template = new JdbcTemplate(getDataSource());
		UserCacheItem item = null;

		try {
			item = (UserCacheItem) template.queryForObject("select objectdata from objectcache where " + _keyName + " = '"
					+ key.toString() + "'", new RowMapper() {

				public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
					ObjectInputStream ois;
					try {
						ois = new ObjectInputStream(new DefaultLobHandler().getBlobAsBinaryStream(rs, 1));
						UserCacheItem dbItem = (UserCacheItem) ois.readObject();
						return dbItem;
					} catch (IOException e) {
						LOGGER.error("Could not get cache item back from database", e);
						return null;
					} catch (ClassNotFoundException e) {
						LOGGER.error("Could not get cache item back from database", e);
						return null;
					}

				}
			});
		} catch (IncorrectResultSizeDataAccessException e) {
			LOGGER.debug("No result found, assume not in cache");
			return null;
		}
		LOGGER.debug("Found item in database cache " + key.toString());

		final int millisInSec = 1000;
		if ((item.getInTime() + (getTimeout() * millisInSec)) > new Date().getTime()) {
			return item;
		}
		remove(key);

		return null;

	}

	public final void put(final SSOToken key, final UserCacheItem value) {
		if (!_databaseEnabled) {
			_delegate.put(key, value);
			return;
		}

		LOGGER.debug("Putting item into database cache under key " + key.toString());

		remove(key);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
		} catch (IOException e) {
			throw new RuntimeException("Could not write object to stream", e);
		}

		SqlUpdate su = new SqlUpdate(getDataSource(), "INSERT INTO objectcache " + "(" + _keyName + ", objectdata,createddate) "
				+ "VALUES (?, ?,?)");
		su.declareParameter(new SqlParameter(_keyName, Types.VARCHAR));
		su.declareParameter(new SqlParameter("objectdata", Types.BLOB));
		su.declareParameter(new SqlParameter("createddate", Types.DATE));
		su.compile();

		final int paramSize = 3;
		Object[] parameterValues = new Object[paramSize];
		parameterValues[0] = key.toString();

		LobHandler lobHandler = new DefaultLobHandler();
		parameterValues[1] = new SqlLobValue(baos.toByteArray(), lobHandler);

		parameterValues[2] = new java.sql.Date(new Date().getTime());

		try {
			su.update(parameterValues);
		} catch (DataIntegrityViolationException e) {
			// DuplicateKeyException isn't introduced until Spring 3 so have to make
			// do with DataIntegrityViolationException
			LOGGER.warn("Insert failed as key ("+key+") may already exist");
		}

	}

	public final void remove(final SSOToken key) {
		if (!_databaseEnabled) {
			_delegate.remove(key);
			return;
		}

		LOGGER.debug("Removing item from database cache " + key.toString());

		SqlUpdate su = new SqlUpdate(getDataSource(), "DELETE FROM objectcache WHERE " + _keyName + " = ?");
		su.declareParameter(new SqlParameter(_keyName, Types.VARCHAR));
		su.compile();
		int results = su.update(new Object[] { key.toString() });

		if (results == 0) {
			LOGGER.debug("No item found in database to remove under key " + key.toString());
		} else {
			LOGGER.debug(results + " item(s) found in database and removed under key " + key.toString());
		}

	}

	/**
	 * @return
	 */
	public final DataSource getDataSource() {
		return _dataSource;
	}

	public final void setDataSource(final DataSource dataSource) {
		_dataSource = dataSource;
	}

	public final int getTimeout() {
		return _timeout;
	}

	public final void setTimeout(final int timeout) {
		_timeout = timeout;
	}

	public final String getKeyName() {
		return _keyName;
	}

	public final void setKeyName(String keyName) {
		_keyName = keyName;
	}

	public final boolean isDatabaseEnabled() {
		return _databaseEnabled;
	}

	public final void setDatabaseEnabled(boolean databaseEnabled) {
		this._databaseEnabled = databaseEnabled;
	}

}
