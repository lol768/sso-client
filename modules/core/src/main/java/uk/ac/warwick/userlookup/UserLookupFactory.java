package uk.ac.warwick.userlookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This can be used instead of UserLookup.getInstance() if
 * you need a factory that returns a different class. It
 * returns the interface rather than the UserLookup object.
 * 
 * By default, returns a UserLookup, but the factory can
 * be reconfigured to return a different class or a different
 * instance.
 * 
 * @author cusebr
 */
public final class UserLookupFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserLookupFactory.class);
	
	private static UserLookupInterface _self;
	
	private static Class<? extends UserLookupInterface> _creationClass = UserLookup.class;
	
	private UserLookupFactory(){}
	
	/**
	 * Obtain a configured UserLookup object.
	 */
	public static synchronized UserLookupInterface getInstance() {
		if (_self == null) {
			LOGGER.warn("UserLookup not initialized - creating new lookup with default settings...");
			try {
				_self = _creationClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException("Couldn't create class", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Couldn't create class", e);
			}
		}
		return _self;
	}
	
	public static synchronized void setInstance(UserLookupInterface instance) {
		_self = instance;
	}
	
	public static synchronized void setInstanceClass(Class<? extends UserLookupInterface> clazz) {
		if (_self != null) {
			throw new IllegalStateException("Can't set instance class as instance has already been created.");
		}
		UserLookupFactory._creationClass = clazz;
	}
	
	/**
	 * Clear the created singleton.
	 */
	static void clear() {
		_self = null;
	}
}
