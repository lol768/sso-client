package uk.ac.warwick.sso.client.trusted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.sso.client.core.HttpRequest;
import uk.ac.warwick.sso.client.core.LinkGenerator;
import uk.ac.warwick.sso.client.core.LinkGeneratorImpl;
import uk.ac.warwick.sso.client.core.Response;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupInterface;
import uk.ac.warwick.util.core.StringUtils;

import javax.inject.Inject;
import java.io.IOException;

public class TrustedApplicationHandlerImpl implements TrustedApplicationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedApplicationHandlerImpl.class);

    private final boolean checkAccountDisabled;

    private UserLookupInterface userLookup;

    private TrustedApplicationsManager appManager;

    private SSOConfiguration config;

    @Inject
    public TrustedApplicationHandlerImpl(
            UserLookupInterface userLookup,
            TrustedApplicationsManager appManager,
            SSOConfiguration config
    ) {
        this.userLookup = userLookup;
        this.appManager = appManager;
        this.config = config;

        this.checkAccountDisabled = config.getBoolean("trustedapps.checkaccountdisabled", false);
    }

    public Response handle(HttpRequest request) throws IOException {
        Response response = new Response();
        response.setContinueRequest(true);

        if (!StringUtils.hasText(request.getHeader(TrustedApplication.HEADER_CERTIFICATE))) {
            // Not a trusted apps request
            return response;
        } else {
            try {
                User user = parseTrustedApplicationsRequest(request);

                response.setUser(user);
                response.setActualUser(user);
                response.setHeader(TrustedApplication.HEADER_STATUS, TrustedApplication.Status.OK.name());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Allowing trusted request for %s on %s", user.getUserId(), getRequestedUrl(request)));
                }
            } catch (TransportException e) {
                // We log this at quite a high level - we don't expect to get errors on this, it's
                // indicative of someone being naughty
                LOGGER.error(
                        String.format(
                                "Failed trusted request on %s: %s (%s)",
                                getRequestedUrl(request),
                                e.getTransportErrorMessage().getCode().getCode(),
                                e.getTransportErrorMessage().getFormattedMessage()
                        )
                );

                response.setStatusCode(500);
                response.setHeader(TrustedApplication.HEADER_STATUS, TrustedApplication.Status.Error.name());
                response.setHeader(TrustedApplication.HEADER_ERROR_CODE, e.getTransportErrorMessage().getCode().getCode());
                response.setHeader(TrustedApplication.HEADER_ERROR_MESSAGE, e.getTransportErrorMessage().getFormattedMessage());
            }
        }

        return response;
    }

    private User parseTrustedApplicationsRequest(HttpRequest request) throws TransportException {
        String requestedUrl = getRequestedUrl(request);

        String certStr = request.getHeader(TrustedApplication.HEADER_CERTIFICATE);

        String providerId = request.getHeader(TrustedApplication.HEADER_PROVIDER_ID);
        if (!StringUtils.hasText(providerId)) {
            throw new FilterException(new TransportErrorMessage.ProviderIdNotFoundInRequest());
        }

        TrustedApplication app = appManager.getTrustedApplication(providerId);
        if (app == null) {
            throw new FilterException(new TransportErrorMessage.ApplicationUnknown(providerId));
        }

        // This will throw an InvalidCertificateException (which is a transport exception)
        ApplicationCertificate certificate =
                app.decode(new EncryptedCertificateImpl(providerId, certStr), request);

        String signature = request.getHeader(TrustedApplication.HEADER_SIGNATURE);

        if (!StringUtils.hasText(signature)) {
            throw new FilterException(new TransportErrorMessage.BadSignature());
        }

        try {
            if (!app.verifySignature(certificate.getCreationTime(), requestedUrl, certificate.getUsername(), signature)) {
                throw new FilterException(new TransportErrorMessage.BadSignature(requestedUrl));
            }
        } catch (SignatureVerificationFailedException e) {
            throw new FilterException(new TransportErrorMessage.BadSignature(requestedUrl));
        }

        User user = userLookup.getUserByUserId(certificate.getUsername());
        if (user != null && user.isFoundUser() && !(checkAccountDisabled && user.isLoginDisabled())) {
            user.setTrustedApplicationsUser(true);

            // Ensure the user is logged in
            user.setIsLoggedIn(true);
        } else if (user != null && user.isLoginDisabled()) {
            throw new FilterException(new TransportErrorMessage.UserDisabled(certificate.getUsername()));
        } else {
            throw new FilterException(new TransportErrorMessage.UserUnknown(certificate.getUsername()));
        }

        return user;
    }

    private String getRequestedUrl(HttpRequest request) {
        LinkGenerator generator = new LinkGeneratorImpl(getConfig(), request);
        return generator.getTarget();
    }

    private static class FilterException extends TransportException {

        FilterException(TransportErrorMessage error) {
            super(error);
        }

    }

    public void setConfig(SSOConfiguration config) {
        this.config = config;
    }

    public SSOConfiguration getConfig() {
        return config;
    }

    public void setUserLookup(UserLookupInterface userLookup) {
        this.userLookup = userLookup;
    }

    public void setTrustedApplicationsManager(TrustedApplicationsManager appManager) {
        this.appManager = appManager;
    }

}
