package uk.ac.warwick.userlookup.webgroups;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.warwick.userlookup.XMLResponseHandler;

/**
 * <p>
 * Implementation which expects an XML stream of groups.
 * </p>
 * 
 * @author xusqac
 */
public final class GroupsInfoXMLResponseHandler extends XMLResponseHandler<GroupInfo> {

	private static final Logger LOGGER = Logger.getLogger(GroupsXMLParser.class);
	
	public GroupsInfoXMLResponseHandler() {
		super(new GroupsInfoXMLParser());
	}

	private GroupInfo info;

	public GroupInfo getResult() {
		return info;
	}

	protected void collectResult(ContentHandler parser) {
		info = ((GroupsInfoXMLParser)parser).getGroupInfo();
	}

	static class GroupsInfoXMLParser extends DefaultHandler {
		
		private boolean failed;
		private String _lastElement;
		private int members;

		public void startElement(final String namespaceURI,
				final String localName, final String qName,
				final Attributes atts) {
			_lastElement = qName;
		}

		public void characters(final char[] data, final int startIndex,
				final int endIndex) {
			if (_lastElement.equals("members")) {
				try {
					members = Integer.parseInt( String.copyValueOf(data, startIndex, endIndex) );
				} catch (NumberFormatException ex) {
					LOGGER.error("Failed to parse the member count", ex);
					failed = true;
				}
			}
		}

		public void endElement(final String namespaceURI,
				final String localName, final String qName) {
			_lastElement = "";
		}

		public GroupInfo getGroupInfo() {
			if (failed) {
				return null;
			}
			return new GroupInfo(members);
		}
	}

	public GroupInfo getGroupInfo() {
		return info;
	}
}
