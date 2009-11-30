/*
 * Created on 18-Apr-2005
 *
 */
package uk.ac.warwick.userlookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class BatchLoadCodesAndPrintUserInfo {

	/**
	 * @param args
	 */
	public static void main(final String[] args) throws Exception {
		BatchLoadCodesAndPrintUserInfo loader = new BatchLoadCodesAndPrintUserInfo();
		loader.process(args[0],args[1]);

	}

	private void process(final String fileName, final String outputFileName) throws Exception {

		UserLookup userlookup = new UserLookup();
		
		userlookup.setSsosUrl("https://websignon.warwick.ac.uk/sentry");
		File file = new File(fileName);
		
		File outputFile = new File(outputFileName);
		FileWriter writer = new FileWriter(outputFile);

		FileReader fr = new FileReader(file);
		BufferedReader reader = new BufferedReader(fr);

		String line = reader.readLine();
		while (line != null) {
			String[] values = line.split(",");
			String author = values[2];
			User user = userlookup.getUserByUserId(author);
			String outputLine = line + "," + user.getEmail() + "," + user.getFullName() + "\n";
			System.out.println(outputLine);
			writer.write(outputLine);
			line = reader.readLine();
		}
		
		writer.close();
		fr.close();
		reader.close();

	}

}
