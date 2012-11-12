package uk.ac.warwick.sso.client.util;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * A set of utilities for parsing well formed XML files.
 * 
 * @author Mat
 */
public abstract class XMLParserUtils {
	
	private static final String DATE_FORMAT = "dd/MM/yy HH:mm:ss";
	
	public static Document parseXmlStream(InputStream stream) {
		try {
			DocumentBuilder builder = Xml.newDocumentBuilder();
			return builder.parse(stream);
		} catch (Exception e) {
			throw new IllegalArgumentException("Error parsing XML stream", e);
		}
	}
	
	public static boolean getBooleanValue(Element el, String elementName) {
		return Boolean.valueOf(getText(getFirstMatchingElement(el, elementName)));
	}
	
	public static Element getFirstMatchingElement(Element el, String name) {
		NodeList matches = el.getElementsByTagName(name);

		if (matches.getLength() > 0) {
			return (Element) matches.item(0);
		}

		return null;
	}

	public static String getText(Node node) {
		if (node == null || node.getChildNodes() == null) {
			return "";
		}

		NodeList children = node.getChildNodes();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			switch (child.getNodeType()) {
			case Node.ELEMENT_NODE:
				if (!child.getNodeName().equalsIgnoreCase("script")) {
					sb.append(getText(child));
					sb.append(" ");
				}
				break;
			case Node.TEXT_NODE:
				sb.append(((Text) child).getData());
				break;
			}
		}
		return sb.toString();
	}

	public static Date getDate(Element element) {
		String text = getText(element);
		
		if (!StringUtils.hasText(text)) {
			return null;
		}
		
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		
		try {
			return df.parse(text);
		} catch (ParseException e) {
			return null;
		}
	}

	public static int getInt(Element element) {
		String text = getText(element);
		
		if (!StringUtils.hasText(text)) {
			return -1;
		}
		
		return Integer.parseInt(text);
	}
	
	public static double getDouble(Element element) {
		String text = getText(element);
		
		if (!StringUtils.hasText(text)) {
			return -1;
		}
		
		return Double.parseDouble(text);
	}

}
