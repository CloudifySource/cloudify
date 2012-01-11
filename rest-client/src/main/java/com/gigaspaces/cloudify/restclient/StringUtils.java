package com.gigaspaces.cloudify.restclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class offers static methods to evaluate or manipulate Strings.
 * 
 * @author rafi
 * @since 8.0.3
 */
public class StringUtils {

	/**
	 * Checks if a given String is not null or empty.
	 * 
	 * @param str
	 *            The String object to evaluate
	 * @return true/false
	 */
	public static boolean notEmpty(final String str) {
		return str != null && str.length() > 0;
	}

	/**
	 * Returns the content of a given input stream, as a String object.
	 * 
	 * @param is
	 *            the input stream to read.
	 * @return the content of the given input stream
	 * @throws IOException
	 */
	public static String getStringFromStream(final InputStream is)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

}
