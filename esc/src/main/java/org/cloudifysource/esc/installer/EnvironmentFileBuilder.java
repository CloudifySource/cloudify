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

import java.util.regex.Pattern;

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
	
	/**
	 * Change all \ char to / from a linux path
	 * @param str
	 * @return
	 */
	public static String normalizeLinuxPath (final String str) {
		String linuxPath=str;
		if (str.contains("\\"))
			linuxPath =  str.replace("\\", "/");
		if (!str.startsWith("/"))
			linuxPath = "/"+linuxPath;
		if (str.endsWith("/"))
			linuxPath = linuxPath.substring(0, linuxPath.length()-1);
		return linuxPath;
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

		switch (this.scriptLanguage) {
		case LINUX_SHELL:
			sb.append("export ").append(name).append("=").append(actualValue);
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
			boolean includesAmpersand = false;
			if (normalizedValue.contains("&") || normalizedValue.contains("%")) {
				normalizedValue = normalizedValue.replace("&", "^&");
				normalizedValue = normalizedValue.replace("%", "%%");
				includesAmpersand = true;
			}

			if (includesAmpersand) {
				sb.append("SET \"").append(name).append("=").append(normalizedValue).append("\"");

			} else {
				// sb.append("$ENV:").append(name).append("=").append(normalizedValue);
				sb.append("SET ").append(name).append("=").append(normalizedValue);
			}
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
