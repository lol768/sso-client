package uk.ac.warwick.userlookup;

import java.util.Map;

public interface UserAttributesAdapter {

    String getEmail();

    String getFullName();

    String getFirstName();

    String getLastName();

    String getUserId();

    String getOldWarwickSSOToken();

    String getDepartment();

    String getDepartmentCode();

    String getDepartmentShortName();

    String getUserType();

    boolean isStaff();

    boolean isStudent();

    boolean isAlumni();

    boolean isLoginDisabled();

    boolean isWarwickPrimary();

    Map<String, String> getAttributes();

    boolean isLoggedIn();

    String getUniversityID();

}
