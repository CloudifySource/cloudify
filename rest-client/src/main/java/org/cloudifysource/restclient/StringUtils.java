/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
 * @since 2.0.0
 */
public final class StringUtils {

    /**
     * Private Ctor, to avoid instantiation of this utility class.
     */
    private StringUtils() {
    }

    /**
     * Checks if a given String is not null or empty.
     *
     * @param str The String object to evaluate
     * @return true/false
     */
    public static boolean notEmpty(final String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Returns the content of a given input stream, as a String object.
     *
     * @param is the input stream to read.
     * @return the content of the given input stream
     * @throws IOException Reporting failure to read from the InputStream
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

	public static Integer safeParseInt(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
	
	/**
	 * Checks if the given name contains chars that are invalid for Application or Service name.
	 * @param name the Application or Service name to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean isValidRecipeName(final String name) {
		char [] invalidChars = new char[] {'{','}','[',']','(',')'};
		return !org.apache.commons.lang.StringUtils.containsAny(name, invalidChars);
	}

}
