package uk.ac.warwick.userlookup.webgroups;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupImpl;

public final class GroupsXMLParser extends DefaultHandler {

	private Collection<Group> _groups = new ArrayList<Group>();

	private String _lastElement;

	private GroupImpl _currentGroup;

	private static final Logger LOGGER = Logger.getLogger(GroupsXMLParser.class);

	public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts) {
		if (qName.equals("group")) {
			_currentGroup = new GroupImpl();
			_currentGroup.setName(atts.getValue("name"));
		} else if (qName.equals("owner")) {
			_currentGroup.getOwners().add(atts.getValue("userId"));
		} else if (qName.equals("user")) {
			_currentGroup.getUserCodes().add(atts.getValue("userId"));
		} else if (qName.equals("subgroup")) {
			_currentGroup.getRelatedGroups().add(atts.getValue("name"));
		} else if (qName.equals("department")) {
			_currentGroup.setDepartmentCode(atts.getValue("code"));
		}
		_lastElement = qName;
	}

	public void characters(final char[] data, final int startIndex, final int endIndex) {
		if (_lastElement.equals("department")) {
			String department = "";
			if (_currentGroup.getDepartment() != null) {
				department = _currentGroup.getDepartment();
			}
			department += String.copyValueOf(data, startIndex, endIndex);
			_currentGroup.setDepartment(department);
		} else if (_lastElement.equals("title")) {
			String title = "";
			if (_currentGroup.getTitle() != null) {
				title = _currentGroup.getTitle();
			}
			title += String.copyValueOf(data, startIndex, endIndex);
			_currentGroup.setTitle(title);
		} else if (_lastElement.equals("lastupdateddate")) {
			String lastUpdatedDate = String.copyValueOf(data, startIndex, endIndex);
			if (!lastUpdatedDate.equals("")) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					_currentGroup.setLastUpdatedDate(sdf.parse(lastUpdatedDate));
				} catch (ParseException e) {
					LOGGER.debug("Could not parse lastupdateddate from web service. Stupid SAX parser");
				}
			}
		} else if (_lastElement.equals("type")) {
			String type = "";
			if (_currentGroup.getType() != null) {
				type = _currentGroup.getTitle();
			}
			type += String.copyValueOf(data, startIndex, endIndex);
			_currentGroup.setType(type);
		}

	}

	public void endElement(final String namespaceURI, final String localName, final String qName) {
		if (qName.equals("group")) {
			if (_currentGroup.getName() != null && !_currentGroup.getName().equals("")) {
				_groups.add(_currentGroup);
			}
		}
		_lastElement = "";
	}

	public Collection<Group> getGroups() {
		return _groups;
	}

}
