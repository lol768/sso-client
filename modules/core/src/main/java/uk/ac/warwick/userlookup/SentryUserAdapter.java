package uk.ac.warwick.userlookup;

import java.util.Map;

public class SentryUserAdapter extends AbstractUserAttributesAdapter {

    private final Map<String, String> attributes;

    public SentryUserAdapter(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    protected String get(String name) {
        return attributes.get(name);
    }

    @Override
    public String getEmail() {
        return get("email");
    }

    @Override
    public String getFullName() {
        return get("name");
    }

    @Override
    public String getFirstName() {
        return get("firstname");
    }

    @Override
    public String getLastName() {
        return get("lastname");
    }

    @Override
    public String getUserId() {
        return get("user");
    }

    @Override
    public String getOldWarwickSSOToken() {
        return get("token");
    }

    @Override
    public String getDepartment() {
        return get("dept");
    }

    @Override
    public String getDepartmentCode() {
        return get("deptcode");
    }

    @Override
    public String getDepartmentShortName() {
        return get("deptshort");
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public String getUniversityID() {
        return get("id");
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }
}
