package uk.ac.warwick.userlookup;

import java.util.Map;

public class SentryUserAdapter implements UserAttributesAdapter {

    private final Map<String, String> attributes;

    public SentryUserAdapter(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    private String get(String name) {
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
    public String getUserType() {
        return get("urn:websignon:usertype");
    }

    @Override
    public boolean isStaff() {
        return "true".equals(get("staff"));
    }

    @Override
    public boolean isStudent() {
        return "true".equals(get("student"));
    }

    @Override
    public boolean isAlumni() {
        return "true".equals(get("alumni")) || "Alumni".equals(getUserType());
    }

    @Override
    public boolean isLoginDisabled() {
        return get("logindisabled") != null && "true".equals(get("logindisabled").toLowerCase());
    }

    @Override
    public boolean isWarwickPrimary() {
        return get("warwickprimary") != null && "yes".equals(get("warwickprimary").toLowerCase());
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
