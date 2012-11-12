/*
 * Created on 14-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.Serializable;

public class SSOToken implements Serializable{
	private static final long serialVersionUID = -685964997789148138L;

	public static final String SSC_TICKET_TYPE = "urn:websignon:ssc";

	public static final String PROXY_TICKET_TYPE = "urn:websignon:proxyticket";

	public static final String PROXY_GRANTING_TICKET_TYPE = "urn:websignon:proxygrantingticket";

	public static final String TGC_TICKET_TYPE = "urn:websignon:tgc";

	public static final String LTC_TICKET_TYPE = "urn:websignon:ltc";

	public static final String UUID_TICKET_TYPE = "urn:websignon:uuid";

	public static final String USERID_TICKET_TYPE = "urn:websignon:userid";

	private String _value;

	private String _type;

	public SSOToken() {
		// default constructor
	}

	public SSOToken(final String value, final String type) {
		_value = value;
		_type = type;
	}

	public final String getType() {
		return _type;
	}

	public final void setType(final String type) {
		_type = type;
	}

	public final String getValue() {
		return _value;
	}

	public final void setValue(final String value) {
		_value = value;
	}

	public final String toString() {
		if (getType() != null) {
			return getValue() + "@" + getType();
		}

		return getValue();
	}

	public final boolean equals(final Object that) {
		if (this == that) {
			return true;
		}

		if (!(that instanceof SSOToken)) {
			return false;
		}

		return that.toString().equals(this.toString());
	}

	public final int hashCode() {
		return toString().hashCode();
	}

}
