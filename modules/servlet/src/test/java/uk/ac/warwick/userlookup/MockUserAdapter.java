package uk.ac.warwick.userlookup;

import java.util.HashMap;
import java.util.Map;

public class MockUserAdapter implements UserAttributesAdapter {
    @Override
    public String getEmail() {
        return "g.blogs@warwick.ac.uk";
    }

    @Override
    public String getFullName() {
        return "Gemma Blogs";
    }

    @Override
    public String getFirstName() {
        return "Gemma";
    }

    @Override
    public String getLastName() {
        return "Blogs";
    }

    @Override
    public String getUserId() {
        return "u1490600";
    }

    @Override
    public String getOldWarwickSSOToken() {
        return "old-WarwickSSO-token";
    }

    @Override
    public String getDepartment() {
        return "IT Services";
    }

    @Override
    public String getDepartmentCode() {
        return "IN";
    }

    @Override
    public String getDepartmentShortName() {
        return "ITS";
    }

    @Override
    public String getUserType() {
        return "Staff";
    }

    @Override
    public boolean isStaff() {
        return true;
    }

    @Override
    public boolean isStudent() {
        return false;
    }

    @Override
    public boolean isAlumni() {
        return false;
    }

    @Override
    public boolean isLoginDisabled() {
        return false;
    }

    @Override
    public boolean isWarwickPrimary() {
        return true;
    }

    @Override
    public Map<String, String> getAttributes() {
        return new HashMap<>();
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public String getUniversityID() {
        return "1490600";
    }

    @Override
    public String getUserSource() {
        return "WarwickADS";
    }
}
