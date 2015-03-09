package uk.ac.warwick.sso.client.trusted;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * TransportErrorMessages are reported when a client makes a TrustedApplication request.
 * <p>
 * The String format of these is important. They basically consist of three elements:
 * <ol>
 * <li>Error Code (one of the {@link Code} constants)
 * <li>Message (a MessageFormat formatted message)
 * <li>Parameters (encoded as a JSON array)
 * </ol>
 * These are separated by a semi-colon tab.
 *
 * @TODO replace inner classes with enum in JDK5
 */
public class TransportErrorMessage {
    // --------------------------------------------------------------------------------------------------------- members

    private final Code code;
    private final String message;
    private final String[] params;

    // ----------------------------------------------------------------------------------------------------------- ctors

    TransportErrorMessage(final Code code, final String message, final String[] params) {
        this.code = code;
        this.message = message;
        this.params = params;
    }

    TransportErrorMessage(final Code code, final String message)
    {
        this(code, message, new String[0]);
    }

    TransportErrorMessage(final Code code, final String message, final String param)
    {
        this(code, message, new String[] { param });
    }

    TransportErrorMessage(final Code code, final String message, final String one, final String two)
    {
        this(code, message, new String[] { one, two });
    }

    TransportErrorMessage(final Code code, final String message, final String one, final String two, final String three)
    {
        this(code, message, new String[] { one, two, three });
    }

    // ------------------------------------------------------------------------------------------------------- accessors

    public Code getCode()
    {
        return code;
    }

    public String[] getParameters()
    {
        return params.clone();
    }

    public String getFormattedMessage()
    {
        return MessageFormat.format(message, (Object[]) params);
    }

    // ------------------------------------------------------------------------------------------------- specializations

    static class System extends TransportErrorMessage {
        System(final Throwable cause, final String appId) {
            super(Code.SYSTEM, "Exception: {0} occurred serving request for application: {1}", cause.toString(), appId);
        }
    }

    /**
     * AppId not found in request
     */
    public static class ProviderIdNotFoundInRequest extends TransportErrorMessage {
        public ProviderIdNotFoundInRequest() {
            super(Code.PROVIDER_ID_NOT_FOUND, "Application ID not found in request");
        }
    }

    public static class ApplicationUnknown extends TransportErrorMessage {
        public ApplicationUnknown(final String providerID)
        {
            super(Code.APP_UNKNOWN, "Unknown Application: {0}", providerID);
        }
    }

    public static class UserUnknown extends TransportErrorMessage {
        public UserUnknown(final String userName)
        {
            super(Code.USER_UNKNOWN, "Unknown User: {0}", userName);
        }
    }

    public static class PermissionDenied extends TransportErrorMessage {
        public PermissionDenied()
        {
            super(Code.PERMISSION_DENIED, "Permission Denied");
        }
    }

    public static class BadSignature extends TransportErrorMessage {
        public BadSignature(String url)
        {
            super(Code.BAD_SIGNATURE, "Bad signature for URL: {0}", url);
        }

        public BadSignature()
        {
            super(Code.BAD_SIGNATURE, "Missing signature in a v2 request");
        }
    }

    // --------------------------------------------------------------------------------------------------- inner classes

    /**
     * Typesafe enum that contains all known error codes.
     * <p>
     * Note: for backwards compatibility, do not ever remove a code once its been released. Deprecate if necessary, but
     * not remove.
     */
    public static final class Code {
        private static final Map<String, Code> ALL = new HashMap<String, Code>();

        public static final Code UNKNOWN = new Code(Severity.ERROR, "UNKNOWN");

        public static final Code APP_UNKNOWN = new Code(Severity.ERROR, "APP_UNKNOWN");
        public static final Code SYSTEM = new Code(Severity.ERROR, "SYSTEM");
        public static final Code PROVIDER_ID_NOT_FOUND = new Code(Severity.ERROR, "PROVIDER_ID_NOT_FOUND");

        public static final Code USER_UNKNOWN = new Code(Severity.ERROR, "USER_UNKNOWN");
        public static final Code PERMISSION_DENIED = new Code(Severity.ERROR, "PERMISSION_DENIED");
        public static final Code BAD_SIGNATURE = new Code(Severity.FAIL, "BAD_SIGNATURE");

        static Code get(final String code) {
            final Code result = ALL.get(code);
            return (result == null) ? Code.UNKNOWN : result;
        }

        private final Severity severity;
        private final String code;

        private Code(final Severity severity, final String code) {
            this.severity = severity;
            this.code = code;
            if (ALL.containsKey(code))
            {
                throw new IllegalArgumentException(code + " is already mapped as a " + this.getClass().getName());
            }
            ALL.put(code, this);
        }

        public Severity getSeverity()
        {
            return severity;
        }

        public String getCode()
        {
            return code;
        }

        public static final class Severity {
            static final Severity ERROR = new Severity("ERROR");
            static final Severity FAIL = new Severity("FAIL");

            private final String name;

            private Severity(final String name)
            {
                this.name = name;
            }

            @Override
            public String toString()
            {
                return name;
            }
        }
    }
}
