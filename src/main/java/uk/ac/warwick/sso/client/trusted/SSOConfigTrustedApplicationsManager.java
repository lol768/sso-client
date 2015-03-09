package uk.ac.warwick.sso.client.trusted;

import com.google.common.collect.ImmutableMap;
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

    private final EncryptionProvider encryptionProvider = new BouncyCastleEncryptionProvider();

    private final CurrentApplication application;

    private final ImmutableMap<String, TrustedApplication> trustedApplications;

    public SSOConfigTrustedApplicationsManager(CurrentApplication application, ImmutableMap<String, TrustedApplication> trustedApplications) {
        this.application = application;
        this.trustedApplications = trustedApplications;
    }

    public SSOConfigTrustedApplicationsManager(SSOConfiguration config) throws Exception {
        // Get current application first by getting the providerID and then the public key
        this.application = new SSOConfigCurrentApplication(config);

        ImmutableMap.Builder<String, TrustedApplication> builder = ImmutableMap.builder();

        Iterator<Pair<String, String>> itr = new PairIterator<>(
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
