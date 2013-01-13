package org.cloudifysource.esc.installer;

/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import java.util.regex.Pattern;

import org.cloudifysource.dsl.cloud.ScriptLanguages;

/*******
 * A simple wrapper around a StringBuilder. Used to generate the contents of the
 * cloudify environment file that is injected into the remote cloudify
 * installation
 *
 * @author dank, barakme
 * @since 2.5.0
 *
 */

public class EnvironmentFileBuilder {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(EnvironmentFileBuilder.class.getName());

	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";
	private static Pattern pattern;

	private final StringBuilder sb = new StringBuilder();
	private String newline;

	private final ScriptLanguages scriptLanguage;

	/********
	 * Constructor.
	 *
	 * @param mode
	 *            the execution mode.
	 */
	public EnvironmentFileBuilder(final ScriptLanguages scriptLanguage) {
		this.scriptLanguage = scriptLanguage;
		switch (scriptLanguage) {
		case LINUX_SHELL:
			this.newline = "\n";
			break;
		case WINDOWS_BATCH:
			this.newline = "\r\n";
			break;
		default:
			throw new UnsupportedOperationException(
					"Unsupported script language: " + scriptLanguage);
		}

	}

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute cifs path,
	 * returns /PATH. If the string does not match, returns the original
	 * unmodified string.
	 *
	 * @param str
	 *            the input path.
	 * @return the input path, adjusted to remove the cifs drive letter, if it
	 *         exists, or the original path if the drive letter is not present.
	 */
	public static String normalizeCifsPath(final String str) {
		final String expression = CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX;
		if (pattern == null) {
			pattern = Pattern.compile(expression);
		}

		if (str == null) {
			return null;
		}
		if (pattern.matcher(str).matches()) {
			final char drive = str.charAt(1);
			return drive + ":\\"
					+ str.substring("/c$/".length()).replace('/', '\\');
		}
		return str;
	}

	/*********
	 * Adds an environment variable to the command line.
	 *
	 * @param name
	 *            variable name.
	 * @param value
	 *            variable value.
	 * @return this.
	 */
	public EnvironmentFileBuilder exportVar(final String name, final String value) {

		logger.fine("exporting var: " + name + " with value " + value);
		String actualValue = value;
		if (value == null) {
			actualValue = "";
		}

		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			sb.append("export ").append(name).append("=").append(actualValue);
			break;
		case WINDOWS_BATCH:

			// remove surrounding quotes
			if (actualValue.startsWith("\"") && actualValue.endsWith("\"")) {
				actualValue = actualValue.substring(1, actualValue.length() - 1);
			}
			final String normalizedValue = normalizeCifsPath(actualValue);

			// sb.append("$ENV:").append(name).append("=").append(normalizedValue);
			sb.append("SET ").append(name).append("=").append(normalizedValue);
			break;

		default:
			throw new IllegalArgumentException("Unsupported script language: " + this.scriptLanguage);
		}

		sb.append(newline);
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	/*******
	 * Returns the file name for the environment file for the current script
	 * language.
	 *
	 * @return the environment file name.
	 */
	public String getEnvironmentFileName() {
		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			return "cloudify_env.sh";
		case WINDOWS_BATCH:
			return "cloudify_env.bat";
		default:
			throw new UnsupportedOperationException("Unexpected script language: " + this.scriptLanguage);
		}
	}

	/********
	 * Export a value that may have special characters and requires quoting.
	 *
	 * @param name
	 *            the variable name.
	 * @param value
	 *            the variable value, may be null.
	 * @return the builder.
	 */
	public EnvironmentFileBuilder exportVarWithQuotes(final String name, final String value) {
		final String actualValue = value != null ? quote(value) : "";
		return this.exportVar(name, actualValue);

	}

	private String quote(final String value) {
		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			return "\"" + value + "\"";
		case WINDOWS_BATCH:
			return value;
		default:
			throw new UnsupportedOperationException("Unexpected script language: " + this.scriptLanguage);
		}
	}

}
