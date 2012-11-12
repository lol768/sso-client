/*
 * Created on 19-May-2005
 *
 */
package uk.ac.warwick.userlookup;

import java.util.Map;

public interface ExtraProperties {

	String WARWICK_YEAR_OF_STUDY = "warwickyearofstudy";

	String WARWICK_COURSE_CODE = "warwickcoursecode";

	String WARWICK_STATUS_CODE = "warwickstatuscode";

	String WARWICK_ATTENDANCE_MODE = "warwickattendancemode";

	String WARWICK_CATEGORY = "warwickcategory";

	String WARWICK_ENROLEMENT_STATUS = "warwickenrolementstatus";

	String WARWICK_FINAL_YEAR = "warwickfinalyear";

	String WARWICK_TEACHING_STAFF = "warwickteachingstaff";

	Map<String, String> getExtraProperties();

	void setExtraProperties(Map<String, String> properties);

	Object getExtraProperty(String key);

}
