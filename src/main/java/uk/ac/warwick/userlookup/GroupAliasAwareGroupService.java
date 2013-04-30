package uk.ac.warwick.userlookup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * GroupService that knows about the magic ANYONE, MEMBER, STAFF, STAFFNOPGR, PGRESEARCH, STUDENTNOPGT, PGTAUGHT, ALUMNI and STUDENT groups.
 *
 * @author xusqac
 */
public final class GroupAliasAwareGroupService extends GroupServiceAdapter implements GroupService {

	public static final String ANYONE = "ANYONE";

    public static final String MEMBER = "MEMBER";

    public static final String STAFF = "STAFF";
    
    public static final String STAFFNOPGR = "STAFFNOPGR";
    
    public static final String ALUMNI = "ALUMNI";
    
    public static final String STUDENT = "STUDENT";
    
    public static final String STUDENTNOPGT = "STUDENTNOPGT";
    
    public static final String PGRESEARCH = "PGRESEARCH";
    
    public static final String PGTAUGHT = "PGTAUGHT";

    private static final String WARWICKITSCLASS_KEY = "warwickitsclass";
	private static final String PGRSTUDENT_CLASS = "PG(R)";
	private static final String PGTSTUDENT_CLASS = "PG(T)";
	private static final String WARWICKCATEGORY_KEY = "warwickcategory";
	private static final String RESEARCH_CATEGORY = "R";
	private static final String TAUGHT_CATEGORY = "T";
	    
    private UserLookup _userLookup;

    public GroupAliasAwareGroupService(final GroupService decorated, final UserLookup userlookup) {
        super(decorated);
        this._userLookup = userlookup;
    }

    public boolean isUserInGroup(final String userId, final String groupName) throws GroupServiceException {
        User user = _userLookup.getUserByUserId(userId);
        if (groupName == null) {
            return false;
        }
        if (ANYONE.equalsIgnoreCase(groupName)) {
            return true;
        }
        if (MEMBER.equalsIgnoreCase(groupName) && (user.isStaff() || user.isStudent())) {
            return true;
        }
        if (STAFF.equalsIgnoreCase(groupName) && user.isStaff()) {
            return true;
        }
        if (STAFFNOPGR.equalsIgnoreCase(groupName) && userIsStaffNotPGR(user)) {
        	return true;
        }
        if (PGRESEARCH.equalsIgnoreCase(groupName) && userIsPGR(user)) {
            return true;
        }
        if (STUDENT.equalsIgnoreCase(groupName) && user.isStudent()) {
            return true;
        }
        if (STUDENTNOPGT.equalsIgnoreCase(groupName) && userIsStudentNotPGT(user)) {
        	return true;
        }
        if (PGTAUGHT.equalsIgnoreCase(groupName) && userIsPGT(user)) {
            return true;
        }
        if (ALUMNI.equalsIgnoreCase(groupName) && user.isAlumni()) {
            return true;
        }
        if (groupName.equalsIgnoreCase(userId)) {
            return true;
        }
        if (groupName.indexOf("*") > 0) {
            String prefix = groupName.substring(0, groupName.indexOf("*"));
            return (userId.startsWith(prefix));
        }
        
		return getDecorated().isUserInGroup(userId, groupName);
		
    }

    private boolean userIsStaffNotPGR(User user) {
		return (user.isStaff() && !hasPGR(user));
	}
    
    private boolean userIsPGR(User user) {
        return (user.isStaff() && hasPGR(user));
    }

	private boolean hasPGR(User user) {
		return PGRSTUDENT_CLASS.equals(user.getExtraProperty(WARWICKITSCLASS_KEY)) 
			|| RESEARCH_CATEGORY.equals(user.getExtraProperty(WARWICKCATEGORY_KEY));
	}

    private boolean userIsStudentNotPGT(User user) {
		return (user.isStudent() && !hasPGT(user));
	}
    
    private boolean userIsPGT(User user) {
        return (user.isStudent() && hasPGT(user));
    }

	private boolean hasPGT(User user) {
		return PGTSTUDENT_CLASS.equals(user.getExtraProperty(WARWICKITSCLASS_KEY)) 
			|| TAUGHT_CATEGORY.equals(user.getExtraProperty(WARWICKCATEGORY_KEY));
	}

	public final List<String> getUserCodesInGroup(final String group) throws GroupServiceException {
        if (ANYONE.equals(group) || MEMBER.equals(group) || STAFF.equals(group)) {
            return Collections.emptyList();
        }

        // treat it as a group if we are a user code, i.e. user codes in its_xusqac are its_xusqac
        if (group.indexOf("-") < 0) {
            return Collections.singletonList(group);
        }

        
		return getDecorated().getUserCodesInGroup(group);
    }

	public Map<String, Set<Cache<?, ?>>> getCaches() {
		return Collections.emptyMap();
	}

	public void clearCaches() {
	}
}
