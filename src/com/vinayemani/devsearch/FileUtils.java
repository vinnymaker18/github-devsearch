package com.vinayemani.devsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vinayemani.devsearch.data.UserData;
import com.vinayemani.devsearch.data.UserKey;

/**
 * FileUtils provides a few utility methods to convert data to/from csv/json files to application data.
 * File extensions are used to determine input/output formats (e.g., 'user.json' for json input and 'data.csv' for
 * csv output) 
 * 
 * @author Vinay E.
 *
 */
public class FileUtils {
	public static List<UserKey> parseFile(String inputFilePath) throws IOException, BadInputFileException {
		File inputFile = new File(inputFilePath);
		String extn = getFileExt(inputFilePath);
		if (extn.equals("csv")) {
			List<UserKey> userKeys = new ArrayList<>();
			FileReader reader = new FileReader(inputFile);
			CSVParser parser = CSVFormat.RFC4180.withQuote('"').withFirstRecordAsHeader().parse(reader);
			Iterator<CSVRecord> iter = parser.iterator();
			while (iter.hasNext()) {
				CSVRecord rec = iter.next();
				String firstName = rec.get("firstName");
				String lastName = rec.get("lastName");
				String location = rec.get("location");
				userKeys.add(new UserKey(firstName, lastName, location));
			}
			reader.close();
			return userKeys;
		} else if (extn.equals("json")) {
			try {
				JSONArray userArray = new JSONArray(readFileContents(inputFilePath));
				List<UserKey> userKeys = new ArrayList<>();
				Iterator<Object> iter = userArray.iterator();
				while (iter.hasNext()) {
					JSONObject user = (JSONObject) iter.next();
					// Expect firstName, lastName and location fields in this object.
					String firstName = user.getString("firstName");
					String lastName = user.getString("lastName");
					String location = user.getString("location");
					userKeys.add(new UserKey(firstName, lastName, location));
				}
				return userKeys;
			} catch (JSONException e) {
				// Error parsing the json file, throw BadInputFileException
				throw new BadInputFileException("File " + inputFilePath + " is badly formed.");
			}
		}
		
		return null;
	}
	
	/**
	 * Write user search results data to a file.
	 * 
	 * @param users User search results.
	 * @param outputFilePath File location to write to.
	 * 
	 * @return True if data is successfully written, false otherwise.
	 * @throws IOException
	 */
	public static boolean writeUserDataToFile(Collection<UserData> users, String outputFilePath) throws IOException {
		Iterator<UserData> iter = users.iterator();
		JSONArray array = new JSONArray();
		while (iter.hasNext()) {
			UserData data = iter.next();
			JSONObject obj = new JSONObject(data);
			array.put(obj);
		}
		FileWriter writer = new FileWriter(outputFilePath);
		writer.write(array.toString(2));
		writer.close();
		return true;
	}
	
	/**
	 * Reads the entire file contents into a string.
	 * 
	 * @param filePath File location.
	 * @return File contents as a string.
	 * 
	 * @throws IOException
	 */
	public static String readFileContents(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		StringBuilder builder = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			builder.append(line + "\n");
		}
		
		reader.close();
		return builder.toString();
	}
	
	/**
	 * Return the extension(e.g., json, csv) of a file path.
	 * 
	 * @param path Path name
	 * @return Extension path of the path.
	 */
	public static String getFileExt(String path) {
		if (path.endsWith(".json")) {
			return "json";
		} else if (path.endsWith(".csv")) {
			return "csv";
		}
		
		return "";
	}
}
