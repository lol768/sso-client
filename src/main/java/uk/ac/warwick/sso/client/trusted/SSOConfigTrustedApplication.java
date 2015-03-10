package uk.ac.warwick.sso.client.trusted;

import java.security.PublicKey;

public class SSOConfigTrustedApplication extends AbstractTrustedApplication {

    public SSOConfigTrustedApplication(String providerID, PublicKey publicKey) {
        super(providerID, publicKey);
    }
}
