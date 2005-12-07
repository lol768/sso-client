/*
 * Created on 06-Dec-2005
 *
 */
package uk.ac.warwick.sso.client.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import uk.ac.warwick.sso.client.SSOToken;

public class DatabaseUserCache implements UserCache {

	private static final Logger LOGGER = Logger.getLogger(DatabaseUserCache.class);

	private DataSource _dataSource;

	public final UserCacheItem get(final SSOToken key) {

		LOGGER.debug("Getting item from database cache " + key.toString());

		JdbcTemplate template = new JdbcTemplate(getDataSource());
		UserCacheItem item = null;

		try {
			item = (UserCacheItem) template.queryForObject("select objectdata from objectcache where key = '" + key.toString()
					+ "'", new RowMapper() {

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
		return item;

	}

	public final void put(final SSOToken key, final UserCacheItem value) {

		LOGGER.debug("Putting item into database cache under key " + key.toString());

		remove(key);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SqlUpdate su = new SqlUpdate(getDataSource(), "INSERT INTO objectcache " + "(key, objectdata,createddate) "
				+ "VALUES (?, ?,?)");
		su.declareParameter(new SqlParameter("key", Types.VARCHAR));
		su.declareParameter(new SqlParameter("objectdata", Types.BLOB));
		su.declareParameter(new SqlParameter("createddate", Types.DATE));
		su.compile();
		
		Object[] parameterValues = new Object[3];
		parameterValues[0] = key.toString();

		LobHandler lobHandler = new DefaultLobHandler();
		parameterValues[1] = new SqlLobValue(baos.toByteArray(), lobHandler);

		parameterValues[2] = new java.sql.Date(new Date().getTime());

		su.update(parameterValues);

	}

	public final void remove(final SSOToken key) {

		LOGGER.debug("Removing item from database cache " + key.toString());

		SqlUpdate su = new SqlUpdate(getDataSource(), "DELETE FROM objectcache WHERE key = '" + key.toString() + "'");
		su.compile();
		int results = su.update();

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

}
