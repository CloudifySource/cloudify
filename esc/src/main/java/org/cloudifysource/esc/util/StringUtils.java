package org.cloudifysource.esc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

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
	 * Splits the string by the specified delimiter and trims the resulting tokens. 
	 * @param stringOfTokens The string to split 
	 * @param delimiter The delimiter to split by
	 * @return A Collection of trimmed String tokens
	 */
	public static Collection<String> splitAndTrimString(String stringOfTokens, String delimiter) {
    	Collection<String> values = new HashSet<String>();
		StringTokenizer tokenizer = new StringTokenizer(stringOfTokens, delimiter);
		while (tokenizer.hasMoreTokens()) {
			values.add(tokenizer.nextToken().trim());
		}
		
		return values;
    }

}
