package org.cloudifysource.restclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class offers static methods to evaluate Strings and read from an input
 * stream.
 * 
 * @author rafi
 * @since 8.0.3
 */
public final class StringUtils {
	
	/**
	 * Private Ctor, to avoid instantiation of this utility class.
	 */
	private StringUtils() { }

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
	 *             Reporting failure to read from the InputStream
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
