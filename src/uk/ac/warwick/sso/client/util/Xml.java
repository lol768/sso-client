package uk.ac.warwick.sso.client.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

public final class Xml {
	private static final Logger LOGGER = Logger.getLogger(Xml.class);
	
	private static boolean supportsSetFeature = true;

	private static ThreadLocal<DocumentBuilderFactory> factory = new ThreadLocal<DocumentBuilderFactory>() {
		protected DocumentBuilderFactory initialValue() {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			factory.set(f);
	        f.setValidating(false);
	        f.setExpandEntityReferences(false);
	        if (supportsSetFeature) {
		        try {
		        	f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		        } catch (AbstractMethodError e) {
		        	// setFeature() not available in xerces 2.6, which is the one that comes with JBoss 3.2.7
		        	LOGGER.warn("Tried to disable external DTD loading but setFeature() is not available. You may need to upgrade Xerces.");
		        	supportsSetFeature = false;
		        } catch (ParserConfigurationException e) {
		        	LOGGER.warn("");
				}
	        }
	        f.setNamespaceAware(true);
	        return f;
		};
	};
	
	/**
	 * Returns a new DocumentBuilder, which doesn't validate but is namespace aware.
	 * It tries to turn off DTD validation if you have a recent Xerces, but will catch the
	 * exception if you are using Xerces 2.6 and just log a warning.
	 */
	public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return factory.get().newDocumentBuilder();
    }
	
	
}
