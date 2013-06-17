package org.cloudifysource.esc.installer;

/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.ScriptLanguages;

/*******
 * A simple wrapper around a StringBuilder. Used to generate the contents of the cloudify environment file that is
 * injected into the remote cloudify installation
 *
 * @author dank, barakme
 * @since 2.5.0
 *
 */

public class EnvironmentFileBuilder {

	private static final String CYGDRIVE = "/cygdrive/";

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(EnvironmentFileBuilder.class.getName());

	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";
	private static Pattern pattern;

	private final StringBuilder sb = new StringBuilder();
	private String newline;

	private final ScriptLanguages scriptLanguage;

	private final Map<String, String> externalEnvVars;
	private final Set<String> appendedExternalEnvVars = new HashSet<String>();

	/********
	 * Constructor.
	 *
	 * @param mode
	 *            the execution mode.
	 */
	public EnvironmentFileBuilder(final ScriptLanguages scriptLanguage, 
								  final Map<String, String> externalEnvVars) {
		this.scriptLanguage = scriptLanguage;
		this.externalEnvVars = externalEnvVars;
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

	private String normalizeWindowsPaths(final String original) {
		final String cifsNormalized = normalizeCifsPath(original);
		final String cygwinNormalized = normalizeCygwinPath(cifsNormalized);
		return cygwinNormalized;

	}

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute cifs path, returns /PATH. If the string does not
	 * match, returns the original unmodified string.
	 *
	 * @param str
	 *            the input path.
	 * @return the input path, adjusted to remove the cifs drive letter, if it exists, or the original path if the drive
	 *         letter is not present.
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

	/********
	 * Normalizes a cygwin path to a standard windows path, where a cygwin path is any string that starts with
	 * '/cygwin/'. Other strings are not changed.
	 *
	 * @param str
	 *            the original value.
	 * @return the normalized string.
	 */
	public static String normalizeCygwinPath(final String str) {
		if (str == null) {
			return null;
		}
		if (!str.startsWith(CYGDRIVE)) {
			return str;
		}

		final String pathWithoutCygdrive = str.substring(CYGDRIVE.length());

		final String pathWithDriveLetter = pathWithoutCygdrive.replaceFirst("/", ":/");
		final String pathWithBackslash = pathWithDriveLetter.replace("/", "\\");

		return pathWithBackslash;
	}

	/********
	 * same as org.cloudifysource.esc.installer.EnvironmentFileBuilder.exportVar(String, String, boolean) with append
	 * field set to false.
	 *
	 * @param name
	 *            name of env var.
	 * @param value
	 *            value of env var.
	 * @return he builder.
	 */
	public EnvironmentFileBuilder exportVar(final String name, final String value) {
		return exportVar(name, value, false);
	}

	/*********
	 * Adds an environment variable to the command line.
	 *
	 * @param name
	 *            variable name.
	 * @param value
	 *            variable value.
	 * @param append
	 *            true if the variable value should be appended to the current one, false otherwise.
	 * @return this.
	 */
	public EnvironmentFileBuilder exportVar(final String name, final String value, final boolean append) {

		String actualValue = value;
		if (value == null) {
			actualValue = "";
		}

		if (append) {
			actualValue = appendValue(name, value);
		}
		// appends the external value, if exists, to the end of the env var.
		// External values override any values that were added via exportVar.
		actualValue = appendExternalValue(name, actualValue);
		
		return addValue(name, actualValue);
	}

	private String appendExternalValue(final String name, final String actualValue) {
		String externalValue = externalEnvVars.get(name);
		String finalValue = actualValue;
		if (!StringUtils.isEmpty(externalValue)) {
			// remove surrounding quotes
			if (externalValue.startsWith("\'") && externalValue.endsWith("\'")) {
				externalValue = externalValue.substring(1, externalValue.length() - 1);
			}
			finalValue = actualValue + ' ' + externalValue;
		}
		appendedExternalEnvVars.add(name);
		return finalValue;
	}

	private EnvironmentFileBuilder addValue(final String name, final String value) {
		String actualValue = value;
		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			sb.append("export ").append('"').append(name).append("=").append(actualValue).append('"');
			break;
		case WINDOWS_BATCH:

			// remove surrounding quotes
			if (actualValue.startsWith("\"") && actualValue.endsWith("\"")) {
				actualValue = actualValue.substring(1, actualValue.length() - 1);
			}

			// If the value of a variable includes an ampersand, it will cause the command to
			// be interpreted as two different commands. To avoid this, escape the ampersand with a caret
			// and wrap the entire command with double quotes. Yes, that is how batch files do it.
			String normalizedValue = normalizeWindowsPaths(actualValue);
			if (normalizedValue.contains("&") || normalizedValue.contains("%")) {
				normalizedValue = normalizedValue.replace("&", "^&");
				normalizedValue = normalizedValue.replace("%", "%%");
			}

			sb.append("SET \"").append(name).append("=").append(normalizedValue).append("\"");

			break;

		default:
			throw new IllegalArgumentException("Unsupported script language: " + this.scriptLanguage);
		}

		sb.append(newline);
		return this;
	}

	private String appendValue(final String name, final String value) {
		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			return "${" + name + "} " + value;

		case WINDOWS_BATCH:
			return "%" + name + "% " + value;

		default:
			throw new IllegalArgumentException("Unsupported script language: " + this.scriptLanguage);
		}

	}
	
	public EnvironmentFileBuilder build() {
		//add only environment vars that were not appended to existing vars.
		for (Map.Entry<String, String> entry : externalEnvVars.entrySet()) { 
			String name = entry.getKey();
			if (!appendedExternalEnvVars.contains(name)) {
				String value = entry.getValue();
				if (!StringUtils.isEmpty(value)) {
					addValue(name, value);
				}
				this.appendedExternalEnvVars.add(name);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	/*******
	 * Returns the file name for the environment file for the current script language.
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
}
