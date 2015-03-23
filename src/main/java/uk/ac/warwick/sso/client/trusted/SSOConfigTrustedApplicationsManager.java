package uk.ac.warwick.sso.client.trusted;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Base64;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.util.collections.Pair;
import uk.ac.warwick.util.collections.PairIterator;

import java.security.PublicKey;
import java.util.Iterator;

/**
 * Default implementation.
 */
public class SSOConfigTrustedApplicationsManager implements TrustedApplicationsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOConfigTrustedApplicationsManager.class);

    private final EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private final CurrentApplication application;

    private final ImmutableMap<String, TrustedApplication> trustedApplications;

    public SSOConfigTrustedApplicationsManager(CurrentApplication application, ImmutableMap<String, TrustedApplication> trustedApplications) {
        this.application = application;
        this.trustedApplications = trustedApplications;
    }

    public SSOConfigTrustedApplicationsManager(SSOConfiguration config) throws Exception {
        // Get current application first by getting the providerID and then the public key
        SSOConfigCurrentApplication currentApp = new SSOConfigCurrentApplication(config);
        this.application = currentApp;

        ImmutableMap.Builder<String, TrustedApplication> builder = ImmutableMap.builder();

        // Trust thyself
        builder.put(this.application.getProviderID(), currentApp);

        Iterator<Pair<String, String>> itr = new PairIterator<String, String>(
            config.getList("trustedapps.app.providerid").iterator(),
            config.getList("trustedapps.app.publickey").iterator()
        );

        while (itr.hasNext()) {
            Pair<String, String> providerIDAndPublicKey = itr.next();
            String providerID = providerIDAndPublicKey.getLeft();

            try {
                PublicKey publicKey = encryptionProvider.toPublicKey(Base64.decode(providerIDAndPublicKey.getRight()));

                builder.put(providerID, new SSOConfigTrustedApplication(providerID, publicKey));
            } catch (Exception e) {
                throw new IllegalStateException("Invalid public key", e);
            }
        }

        this.trustedApplications = builder.build();

        LOGGER.info(
            String.format(
                "SSOConfigTrustedApplicationsManager initialised. Current application: %s, public key: %s",
                application.getProviderID(),
                new String(Base64.encode(application.getPublicKey().getEncoded()), "UTF-8")
            )
        );
    }

    public CurrentApplication getCurrentApplication()
    {
        return application;
    }

    public TrustedApplication getTrustedApplication(final String id)
    {
        return trustedApplications.get(id);
    }

}
