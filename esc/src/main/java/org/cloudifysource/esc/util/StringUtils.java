package org.cloudifysource.esc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * A utility class for string manipulation.
 * 
 * @author noak
 * @since 2.3.1
 */
public class StringUtils {
	
	/**
	 * Returns the content of a given input stream, as a String object.
	 * 
	 * @param is the input stream to read.
	 * @return the content of the given input stream
	 * @throws IOException Reporting failure to read from the InputStream
	 */
	public static String getStringFromStream(final InputStream is)
			throws IOException {
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	/**
	 * Converts a json String to a Map<String, Object>.
	 * 
	 * @param response a json-format String to convert to a map
	 * @return a Map<String, Object> based on the given String
	 * @throws IOException Reporting failure to read or map the String
	 */
	public static Map<String, Object> jsonToMap(final String response)
			throws IOException {
		@SuppressWarnings("deprecation")
		final JavaType javaType = TypeFactory.type(Map.class);
		return new ObjectMapper().readValue(response, javaType);
	}
	
	
	/**
	 * Converts a given array of String values to a single String of array items separated by a delimiter.
	 * @param strArray The Array of items to concatenate
	 * @param delimiter The delimiter to use
	 * @return A String of array items separated by a delimiter
	 */
	public static String arrayToString(final String[] strArray, final String delimiter) {
		String result = "";
		if (strArray != null) {
			for (String item : strArray) {
				if (result.length() > 0) {
					result += delimiter;
				}
				result += item;
			}
		}
		
		return result;
	}

}
