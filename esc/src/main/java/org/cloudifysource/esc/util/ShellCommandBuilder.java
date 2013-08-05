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
package org.cloudifysource.esc.util;

import java.util.regex.Pattern;

import org.cloudifysource.domain.cloud.RemoteExecutionModes;

/*******
 * A simple wrapper around a StringBuilder. Used to generate the command line
 * required to run the remote Cloudify agent.
 * 
 * @author dank, barakme
 * @since 1.0
 * 
 */
public class ShellCommandBuilder {

	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";
	private static final String SSH_COMMAND_SEPARATOR = ";";
	private static final String POWERSHELL_COMMAND_SEPARATOR = ";"; // System.getProperty("line.separator");
	private static Pattern pattern;

	private final StringBuilder sb = new StringBuilder();
	private String separator;

	/******
	 * Enum for supported script languages.
	 * 
	 * @author barakme
	 * 
	 */
	public enum ScriptLanguages {
		/**
		 * Linux shell script.
		 */
		LINUX_SHELL,
		/****
		 * Windows powershell.
		 */
		WINDOWS_POWERSHELL

	}

	private boolean runInBackground = false;
	private RemoteExecutionModes mode = RemoteExecutionModes.SSH;

	/********
	 * Constructor.
	 * 
	 * @param mode
	 *            the execution mode.
	 */
	public ShellCommandBuilder(final RemoteExecutionModes mode) {
		this.mode = mode;
		switch (mode) {
		case SSH:
			this.separator = SSH_COMMAND_SEPARATOR;
			break;
		case WINRM:
			this.separator = POWERSHELL_COMMAND_SEPARATOR;
			break;
		default:
			throw new UnsupportedOperationException(
					"Unsupported execution mode: " + mode);
		}

	}

	/******
	 * Default constructor, using LINUX_SHELL.
	 */
	public ShellCommandBuilder() {
		this(RemoteExecutionModes.SSH);
	}

	/*******
	 * Adds a command to the command line.
	 * 
	 * @param str
	 *            the command to add.
	 * @return this.
	 */
	public ShellCommandBuilder call(final String str) {
		if (this.mode == RemoteExecutionModes.SSH) {
			sb.append(str);
		} else {
			final String normalizedPathCommand = normalizeCifsPath(str);
			sb.append(normalizedPathCommand);
		}

		return this;
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

	/********
	 * Adds a separator.
	 * 
	 * @return this.
	 */
	public ShellCommandBuilder separate() {
		sb.append(this.separator);
		return this;
	}

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ShellCommandBuilder.class.getName());

	/*********
	 * Adds an environment variable to the command line.
	 * 
	 * @param name
	 *            variable name.
	 * @param value
	 *            variable value.
	 * @return this.
	 */
	public ShellCommandBuilder exportVar(final String name, final String value) {

		logger.fine("exporting var: " + name + " with value " + value);
		String actualValue = value;
		if (value == null) {
			actualValue = "";
		}

		switch (this.mode) {
		case SSH:
			sb.append("export ").append(name).append("=").append(actualValue);
			break;
		case WINRM:
			String normalizedValue = normalizeCifsPath(actualValue);
			if (normalizedValue.startsWith("\"") && normalizedValue.endsWith("\"")) {
				normalizedValue = normalizedValue.replace("\"", "'");
			} else {
				if (!(normalizedValue.startsWith("\"") && normalizedValue.endsWith("\""))) {
					normalizedValue = "'" + normalizedValue + "'";
				}
			}

			sb.append("$ENV:").append(name).append("=").append(normalizedValue);
			break;

		default:
			// not possible.
			break;
		}

		separate();
		return this;
	}

	/******
	 * Marks a file as executable.
	 * 
	 * @param path
	 *            the file path.
	 * @return this.
	 */
	public ShellCommandBuilder chmodExecutable(final String path) {
		switch (this.mode) {
		case SSH:
			sb.append("chmod +x ").append(path);
			separate();
			break;

		default:
			break;
		}

		return this;
	}

	/*****
	 * Marks a command line to be executed in the background.
	 * 
	 * @return this.
	 */
	public ShellCommandBuilder runInBackground() {
		switch (this.mode) {
		case SSH:
			sb.append(" &");
			break;
		default:
			break;
		}

		this.runInBackground = true;

		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	public boolean isRunInBackground() {
		return runInBackground;
	}

}
