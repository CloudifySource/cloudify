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
 *******************************************************************************/
package org.cloudifysource.usm.launcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/******************
 * A utility class to handle exception thrown from groovy closures and parsers.
 *
 * @author adaml
 *
 */
public final class GroovyExceptionHandler {

	private GroovyExceptionHandler() {
		// private constructor to prevent initialization.
	}

	private static final String COMPILATION_EXCEPTION_CAUGHT_REGEX =
			"(([a-zA-Z]*:\\\\|/).+\\.groovy:\\s[1-9]+.*column\\s[1-9]+)";
	private static Pattern compilationPattern = null;

	/**
	 *
	 * Extracts the groovy exception string from a given string. Currently supports groovy Runtime and Compilation
	 * exceptions.
	 *
	 * @param input
	 *            The string containing the groovy exception.
	 * @return Groovy exception string. empty string if not found.
	 */
	public static String getExceptionString(final String input) {
		String exceptionReason = getCompilationException(input);
		if (exceptionReason.isEmpty()) {
			// No compilation exception was found. Return the entire process output.
			exceptionReason = input;
		}
		return exceptionReason;
	}

	/**
	 * returns the groovy compilation exception from a given string.
	 *
	 * @param input
	 * @return The compilation exception string, empty string if not found.
	 */
	private static String getCompilationException(final String input) {
		return getCompilationPatternMatch(
				input, COMPILATION_EXCEPTION_CAUGHT_REGEX, Pattern.MULTILINE);
	}

	private static String getCompilationPatternMatch(final String input, final String regex, final int regexFlags) {

		if (input == null) {
			return "";
		}
		// create the pattern using the regex and flags.
		// this pattern will only be used in cases where a groovy exception occurs
		// So creating the pattern will be done locally.
		if (compilationPattern == null) {
			compilationPattern = Pattern.compile(
					regex, regexFlags);
		}
		final Matcher matcher = compilationPattern.matcher(input);
		int beginIndex = 0;
		int endIndex = 0;
		if (matcher.find()) {
			beginIndex = matcher.start(0);
			endIndex = matcher.end(0);
		}
		return input.substring(
				beginIndex, endIndex);
	}
}
