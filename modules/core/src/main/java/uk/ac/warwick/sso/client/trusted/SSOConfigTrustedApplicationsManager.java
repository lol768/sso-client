package uk.ac.warwick.sso.client.trusted;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Base64;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.util.collections.Pair;
import uk.ac.warwick.util.collections.PairIterator;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation.
 */
public class SSOConfigTrustedApplicationsManager implements TrustedApplicationsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOConfigTrustedApplicationsManager.class);

    private final EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private boolean disabled;

    private final CurrentApplication application;

    private final ImmutableMap<String, TrustedApplication> trustedApplications;

    public SSOConfigTrustedApplicationsManager(CurrentApplication application, ImmutableMap<String, TrustedApplication> trustedApplications) {
        this.application = application;
        this.trustedApplications = trustedApplications;
    }

    public SSOConfigTrustedApplicationsManager(SSOConfiguration config) throws Exception {
        if (!config.getBoolean("trustedapps.enabled", true)) {
            // We allow disabling so the SSOClientModule can load the manager unconditionally -
            // if an app doesn't want trustedapps support, it can mark it as disabled.
            disabled = true;
            application = null;
            trustedApplications = null;
            return;
        }

        // Get current application first by getting the providerID and then the public key
        SSOConfigCurrentApplication currentApp = new SSOConfigCurrentApplication(config);
        this.application = currentApp;

        ImmutableMap.Builder<String, TrustedApplication> builder = ImmutableMap.builder();

        // Trust thyself
        builder.put(this.application.getProviderID(), currentApp);
        
        List<Pair<String, String>> pairs = Lists.newArrayList(PairIterator.of(
            Arrays.asList(config.getStringArray("trustedapps.app.providerid")),
            Arrays.asList(config.getStringArray("trustedapps.app.publickey"))
        ));

        for (Pair<String, String> providerIDAndPublicKey : pairs) {
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
