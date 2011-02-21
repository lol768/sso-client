package uk.ac.warwick.sso.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4J appender that saves stuff to a list, so that we can
 * unit test stuff that gets logged.
 */
public class TestLogAppender extends AppenderSkeleton {

	private static List<String> messages = new ArrayList<String>();
	
	public void clear() {
		messages.clear();
	}
	
	public boolean contains(String... strings) {
		outer: for (String line : messages) {
			for (String s : strings) {
				if (!line.contains(s)) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	protected void append(LoggingEvent arg0) {
		messages.add(arg0.getRenderedMessage());
		
		System.err.println(arg0.getRenderedMessage());
	}

	public void close() {}

	public boolean requiresLayout() {
		return false;
	}

}
