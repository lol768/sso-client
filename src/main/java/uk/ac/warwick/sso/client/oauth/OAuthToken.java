package uk.ac.warwick.sso.client.oauth;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class OAuthToken implements Serializable, Cloneable {

    private static final long serialVersionUID = 5925205291368946976L;

    public static enum Type {
        REQUEST, ACCESS, DISABLED
    }
    
    public static enum RequestedExpiry {
    	ONE_YEAR, FOREVER
    }

    private String token;

    private String tokenSecret;

    private String consumerKey;

    private String oauthVersion;

    private String callbackUrl;

    private boolean callbackUrlSigned;

    private String callbackToken;

    private boolean authorised;

    private Date issueTime;

    private Type type;

    private String userId;

    // scope
    private String service;
    
    private Date expiresAt;
    
    private RequestedExpiry requestedExpiry;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getOAuthVersion() {
        return oauthVersion;
    }

    public void setOAuthVersion(String oauthVersion) {
        this.oauthVersion = oauthVersion;
    }

    public boolean isCallbackUrlSigned() {
        return callbackUrlSigned;
    }

    public void setCallbackUrlSigned(boolean callbackUrlSigned) {
        this.callbackUrlSigned = callbackUrlSigned;
    }

    public Date getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Date issueTime) {
        this.issueTime = issueTime;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getCallbackToken() {
        return callbackToken;
    }

    public void setCallbackToken(String callbackToken) {
        this.callbackToken = callbackToken;
    }

    public boolean isAuthorised() {
        return authorised;
    }

    public void setAuthorised(boolean authorised) {
        this.authorised = authorised;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public final Date getExpiresAt() {
        return expiresAt;
    }

    public final void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        Date currentDate = new Date();
        return currentDate.compareTo(this.getExpiresAt()) > 0;
    }
    
    @Override
    public OAuthToken clone() throws CloneNotSupportedException {
    	OAuthToken clone = (OAuthToken) super.clone();
    	clone.expiresAt = (Date) expiresAt.clone();
    	clone.issueTime = (Date) issueTime.clone();
		return clone;
    }
    
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<String, String>();
        
        map.put("authorised", Boolean.toString(isAuthorised()));
        map.put("callback_token", getCallbackToken());
        map.put("callback_url", getCallbackUrl());
        map.put("callback_url_signed", Boolean.toString(isCallbackUrlSigned()));
        map.put("consumer_key", getConsumerKey());
        
        if (getExpiresAt() != null) {
            map.put("expires_at", Long.toString(getExpiresAt().getTime()));
        }
        
        if (getIssueTime() != null) {
            map.put("issue_time", Long.toString(getIssueTime().getTime()));
        }
        
        map.put("oauth_version", getOAuthVersion());
        map.put("service", getService());
        map.put("token", getToken());
        map.put("token_secret", getTokenSecret());
        map.put("type", getType().toString());
        map.put("user_id", getUserId());
        
        return map;
    }
    
    public static OAuthToken fromMap(Map<String, String> attributes) {
        OAuthToken token = new OAuthToken();
        token.setAuthorised("true".equals(attributes.get("authorised")));
        token.setCallbackToken(attributes.get("callback_token"));
        token.setCallbackUrl(attributes.get("callback_url"));
        token.setCallbackUrlSigned("true".equals(attributes.get("callback_url_signed")));
        token.setConsumerKey(attributes.get("consumer_key"));
        
        if (attributes.containsKey("expires_at")) {
            token.setExpiresAt(new Date(Long.parseLong(attributes.get("expires_at"))));
        }
        
        if (attributes.containsKey("issue_time")) {
            token.setIssueTime(new Date(Long.parseLong(attributes.get("issue_time"))));
        }
        
        token.setOAuthVersion(attributes.get("oauth_version"));
        token.setService(attributes.get("service"));
        token.setToken(attributes.get("token"));
        token.setTokenSecret(attributes.get("token_secret"));
        token.setType(Type.valueOf(attributes.get("type")));
        token.setUserId(attributes.get("user_id"));
        return token;
    }

	public RequestedExpiry getRequestedExpiry() {
		return requestedExpiry;
	}

	public void setRequestedExpiry(RequestedExpiry requestedExpiry) {
		this.requestedExpiry = requestedExpiry;
	}

}
