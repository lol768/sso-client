package uk.ac.warwick.userlookup;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.warwick.userlookup.HttpMethodWebService.HandlerException;

/**
 * WebServiceResponseHandler that takes a SAX handler to parse the returned
 * stream (of XML).
 */
public abstract class XMLResponseHandler<T> extends ClearGroupResponseHandler implements ResultAwareWebServiceResponseHandler<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLResponseHandler.class);

	private ContentHandler _parser;
	
	public XMLResponseHandler(DefaultHandler handler) {
		this._parser = handler;
	}

	public final void processResults(final HttpResponse response) throws HandlerException {
		LOGGER.debug("Parsing XML result from server");
		doParse(response, _parser);
		LOGGER.debug("Finished parsing XML result from server");
		collectResult(_parser);
	}
	
	/**
	 * Called after parsing for implementations to get any information they
	 * need from the handler.
	 * @param parser the parser passed in to XMLResponseHandler
	 */
	protected abstract void collectResult(ContentHandler parser);

	private final void doParse(final HttpResponse response, final ContentHandler xmlParser) {

		SAXParserFactory factory = SAXParserFactory.newInstance();		
		try {
			factory.setFeature("http://xml.org/sax/features/validation", false);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(xmlParser);
			reader.parse(new InputSource(response.getEntity().getContent()));
		} catch (final IOException e) {
			LOGGER.error("IOException: " + e.getMessage());
		} catch (final ParserConfigurationException e) {
			LOGGER.error("ParserConfigurationException: " + e.getMessage());
		} catch (final SAXException e) {
			LOGGER.error("SAXException: " + e.getMessage());
		}
	}
}
