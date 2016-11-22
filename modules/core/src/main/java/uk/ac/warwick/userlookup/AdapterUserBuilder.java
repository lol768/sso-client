package uk.ac.warwick.userlookup;

public class AdapterUserBuilder {

    public static User buildUser(UserAttributesAdapter a) {
        User user = new User();

        user.setEmail(a.getEmail());
        user.setFullName(a.getFullName());
        user.setFirstName(a.getFirstName());
        user.setLastName(a.getLastName());
        user.setUserId(a.getUserId());
        user.setOldWarwickSSOToken(a.getOldWarwickSSOToken());
        user.setDepartment(a.getDepartment());
        user.setDepartmentCode(a.getDepartmentCode());
        user.setShortDepartment(a.getDepartmentShortName());
        user.setUserType(a.getUserType());
        user.setStaff(a.isStaff());
        user.setStudent(a.isStudent());
        user.setAlumni(a.isAlumni());
        user.setLoginDisabled(a.isLoginDisabled());
        user.setWarwickId(a.getUniversityID());
        user.setExtraProperties(a.getAttributes());
        user.setIsLoggedIn(a.isLoggedIn());
        user.setFoundUser(true);

        return user;
    }

}
