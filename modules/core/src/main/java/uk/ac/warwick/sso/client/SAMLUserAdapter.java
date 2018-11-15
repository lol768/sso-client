package uk.ac.warwick.sso.client;

import org.opensaml.SAMLAttribute;
import uk.ac.warwick.userlookup.AbstractUserAttributesAdapter;
import uk.ac.warwick.userlookup.UserAttributesAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SAMLUserAdapter extends AbstractUserAttributesAdapter {

    private final Properties attributes;

    public SAMLUserAdapter(Properties attributes) {
        this.attributes = attributes;
    }

    protected String get(String name) {
        if (attributes.get(name) == null) {
            return null;
        }

        return (String) ((SAMLAttribute) attributes.get(name)).getValues().next();
    }

    @Override
    public String getEmail() {
        return get("mail");
    }

    @Override
    public String getFullName() {
        return null;
    }

    @Override
    public String getFirstName() {
        return get("givenName");
    }

    @Override
    public String getLastName() {
        return get("sn");
    }

    @Override
    public String getUserId() {
        return get("cn");
    }

    @Override
    public String getOldWarwickSSOToken() {
        return null;
    }

    @Override
    public String getDepartment() {
        return get("ou");
    }

    @Override
    public String getDepartmentCode() {
        return get("warwickdeptcode");
    }

    @Override
    public String getDepartmentShortName() {
        return get("deptshort");
    }


    @Override
    public boolean isLoggedIn() {
        return "true".equals(get("urn:websignon:loggedin"));
    }

    @Override
    public String getUniversityID() {
        return get("warwickuniid");
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> map = new HashMap<>();

        for (Object key : attributes.keySet()) {
            String name = (String) key;
            String value = get(name);
            map.put(name, value);
        }

        return map;
    }
}
