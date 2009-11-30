package uk.ac.warwick.userlookup;

public final class WebServiceTimeoutConfig {

	private int _httpConnectionTimeout;

	private int _httpDataTimeout;
	
	public WebServiceTimeoutConfig() {
		// nothing needed in default constructor
	}

	public WebServiceTimeoutConfig(int connectionTimeout, int dataTimeout) {
		if (connectionTimeout == 0) {
			if (UserLookup.getConfigProperty("userlookup.connectiontimeout") != null) {
				_httpConnectionTimeout = Integer.parseInt(UserLookup.getConfigProperty("userlookup.connectiontimeout"));
			} else {
				_httpConnectionTimeout = UserLookup.DEFAULT_CONNECTION_TIMEOUT;
			}
		} else {
			_httpConnectionTimeout = connectionTimeout;
		}
		if (dataTimeout == 0) {
			if (UserLookup.getConfigProperty("userlookup.datatimeout") != null) {
				_httpDataTimeout = Integer.parseInt(UserLookup.getConfigProperty("userlookup.datatimeout"));
			} else {
				_httpDataTimeout = UserLookup.DEFAULT_DATA_TIMEOUT;
			}
		} else {
			_httpDataTimeout = dataTimeout;
		}
	}

	public int getConnectionTimeout() {
		return _httpConnectionTimeout;
	}

	public int getDataTimeout() {
		return _httpDataTimeout;
	}

	public void setConnectionTimeout(int timeout) {
		_httpConnectionTimeout = timeout;
	}

	public void setDataTimeout(int timeout) {
		_httpDataTimeout = timeout;
	}

}