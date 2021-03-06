package uk.ac.warwick.sso.client.trusted;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bouncycastle.util.encoders.Base64;
import uk.ac.warwick.sso.client.SSOConfiguration;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.SingularCacheEntryFactory;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.PrivateKey;

import static java.lang.Integer.*;
import static uk.ac.warwick.userlookup.UserLookup.*;

public class SSOConfigCurrentApplication extends AbstractTrustedApplication implements CurrentApplication {

    public static final String CERTIFICATE_CACHE_NAME = "CurrentApplicationCertificateCache";

    private final PrivateKey privateKey;

    // Note that we can't cache for very long, because otherwise the certificate will be stale!
    private final Cache<CacheKey, EncryptedCertificate> cache;

    public SSOConfigCurrentApplication(SSOConfiguration config) throws Exception {
        super(config);

        cache = Caches.newCache(
                CERTIFICATE_CACHE_NAME,
                new CacheEntryFactory(),
                config.getInt("ssoclient.trusted.cache.certificate.timeout.secs"),
                Caches.CacheStrategy.valueOf(config.getString("ssoclient.cache.strategy")),
                config.getCacheProperties()
        );

        this.privateKey = encryptionProvider.toPrivateKey(Base64.decode(config.getString("trustedapps.privatekey")));
    }

    @Override
    public EncryptedCertificate encode(String username, String urlToSign) {
        try {
            return cache.get(new CacheKey(username, urlToSign));
        } catch (CacheEntryUpdateException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class CacheKey implements Serializable {

        private final String username;

        private final String urlToSign;

        CacheKey(String username, String urlToSign) {
            this.username = username;
            this.urlToSign = urlToSign;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append("username", username)
                .append("urlToSign", urlToSign)
                .toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CacheKey)) {
                return false;
            }

            CacheKey other = (CacheKey) obj;
            return new EqualsBuilder()
                .append(username, other.username)
                .append(urlToSign, other.urlToSign)
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(username)
                .append(urlToSign)
                .toHashCode();
        }
    }

    private class CacheEntryFactory extends SingularCacheEntryFactory<CacheKey, EncryptedCertificate> {
        @Override
        public EncryptedCertificate create(CacheKey key) throws CacheEntryUpdateException {
            try {
                return encryptionProvider.createEncryptedCertificate(key.username, privateKey, getProviderID(), key.urlToSign);
            } catch (Exception e) {
                throw new CacheEntryUpdateException(e);
            }
        }

        @Override
        public boolean shouldBeCached(EncryptedCertificate val) {
            return true;
        }
    }

}
