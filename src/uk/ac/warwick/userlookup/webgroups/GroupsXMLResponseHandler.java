package uk.ac.warwick.userlookup.webgroups;

import java.util.ArrayList;
import java.util.Collection;

import org.xml.sax.ContentHandler;

import uk.ac.warwick.userlookup.XMLResponseHandler;

/**
 * <p>
 * Implementation which expects an XML stream of groups.
 * </p>
 * 
 * @author xusqac
 */
public final class GroupsXMLResponseHandler extends XMLResponseHandler {

	public GroupsXMLResponseHandler() {
		super(new GroupsXMLParser());
	}

	private Collection groups = new ArrayList();

	public Object getResult() {
		return groups;
	}

	protected void collectResult(ContentHandler parser) {
		groups = ((GroupsXMLParser)parser).getGroups();
	}
}
