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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.IPUtils;

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

	private static final String GSA_MODE_ENV = "GSA_MODE";

	private static final String NO_WEB_SERVICES_ENV = "NO_WEB_SERVICES";
	
	private static final String NO_MANAGEMENT_SPACE_ENV = "NO_MANAGEMENT_SPACE";
	
	private static final String NO_MANAGEMENT_SPACE_CONTAINER_ENV = "NO_MANAGEMENT_SPACE_CONTAINER";

	private static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS";

	private static final String WORKING_HOME_DIRECTORY_ENV = "WORKING_HOME_DIRECTORY";

	private static final String CLOUD_FILE = "CLOUD_FILE";

	private static final String AUTO_RESTART_AGENT = "AUTO_RESTART_AGENT";

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

	private String createSpringProfilesString(final InstallationDetails details) {
		final String securityProfile = details.getSecurityProfile();
		final String storageProfile =
				(details.isPersistent() ? CloudifyConstants.PERSISTENCE_PROFILE_PERSISTENT
						: CloudifyConstants.PERSISTENCE_PROFILE_TRANSIENT);

		return securityProfile + "," + storageProfile;

	}

	/*********
	 * 8 Loads the environment file from the given installation details.
	 * 
	 * @param details
	 *            the installation details.
	 */
	public void loadEnvironmentFileFromDetails(final InstallationDetails details) {

		final EnvironmentFileBuilder builder = this;

		String remoteDirectory = details.getRemoteDir();
		if (remoteDirectory.endsWith("/")) {
			remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
		}
		if (details.isManagement()) {
			// add the relative path to the cloud file location
			remoteDirectory = remoteDirectory + "/" + details.getRelativeLocalDir();
		}

		String authGroups = null;
		if (details.getAuthGroups() != null) {
			// authgroups should be a strongly typed object convertible into a
			// String
			authGroups = details.getAuthGroups();
		}

		final String springProfiles = createSpringProfilesString(details);

		String safePublicIpAddress = IPUtils.getSafeIpAddress(details.getPublicIp());
		String safePrivateIpAddress = IPUtils.getSafeIpAddress(details.getPrivateIp());

		builder.exportVar(LUS_IP_ADDRESS_ENV, details.getLocator())
				.exportVar(GSA_MODE_ENV, details.isManagement() ? "lus" : "agent")
				.exportVar(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR, springProfiles)
				.exportVar(NO_WEB_SERVICES_ENV,
						details.isNoWebServices() ? "true" : "false")
				.exportVar(NO_MANAGEMENT_SPACE_ENV,
						details.isNoManagementSpace() ? "true" : "false")
				.exportVar(NO_MANAGEMENT_SPACE_CONTAINER_ENV,
						details.isNoManagementSpaceContainer() ? "true" : "false")
				.exportVar(
						CloudifyConstants.CLOUDIFY_CLOUD_MACHINE_IP_ADDRESS_ENV,
						details.isBindToPrivateIp() ? safePrivateIpAddress : safePublicIpAddress)
				.exportVar(CloudifyConstants.CLOUDIFY_LINK_ENV,
						details.getCloudifyUrl())
				.exportVar(CloudifyConstants.CLOUDIFY_OVERRIDES_LINK_ENV,
						details.getOverridesUrl())
				.exportVar(WORKING_HOME_DIRECTORY_ENV, remoteDirectory)
				.exportVar(AUTO_RESTART_AGENT, details.getAutoRestartAgent())
				.exportVar(CloudifyConstants.GIGASPACES_AUTH_GROUPS, authGroups)
				.exportVar(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP, safePrivateIpAddress)
				.exportVar(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP, safePublicIpAddress)
				.exportVar(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME, details.getTemplateName())
				.exportVar(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID, details.getMachineId())
				.exportVar(CloudifyConstants.CLOUDIFY_CLOUD_MACHINE_ID, details.getMachineId())
				// maintain backwards compatibility for pre 2.3.0
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP, safePrivateIpAddress)
				.exportVar(CloudifyConstants.CLOUDIFY_CLOUD_LOCATION_ID, details.getLocationId())
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP, safePublicIpAddress);

		if (details.isManagement()) {
			String remotePath = details.getRemoteDir();
			if (!remotePath.endsWith("/")) {
				remotePath += "/";
			}
			builder.exportVar(CLOUD_FILE, remotePath + details.getCloudFile().getName());

			logger.log(Level.FINE, "Setting ESM/GSM/LUS java options.");
			builder.exportVar("ESM_JAVA_OPTIONS", details.getEsmCommandlineArgs());
			builder.exportVar("LUS_JAVA_OPTIONS", details.getLusCommandlineArgs());
			builder.exportVar("GSM_JAVA_OPTIONS", details.getGsmCommandlineArgs());

			logger.log(Level.FINE, "Setting gsc lrmi port-range and custom rest/webui ports.");
			builder.exportVar(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR, details.getGscLrmiPortRange());
			builder.exportVar(CloudifyConstants.REST_PORT_ENV_VAR, details.getRestPort().toString());
			builder.exportVar(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR, details.getRestMaxMemory());
			builder.exportVar(CloudifyConstants.WEBUI_PORT_ENV_VAR, details.getWebuiPort().toString());
			builder.exportVar(CloudifyConstants.WEBUI_MAX_MEMORY_ENVIRONMENT_VAR, details.getWebuiMaxMemory());
		}
		logger.log(Level.FINE, "Setting GSA java options.");
		builder.exportVar("GSA_JAVA_OPTIONS", details.getGsaCommandlineArgs());

		if (details.getUsername() != null) {
			builder.exportVar("USERNAME", details.getUsername());
		}
		if (details.getPassword() != null) {
			builder.exportVar("PASSWORD", details.getPassword());
		}

		builder.exportVar(CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR, details.getRemoteDir()
				+ "/" + CloudifyConstants.SECURITY_FILE_NAME);
		if (StringUtils.isNotBlank(details.getKeystorePassword())) {
			builder.exportVar(CloudifyConstants.KEYSTORE_FILE_ENV_VAR, details.getRemoteDir()
					+ "/" + CloudifyConstants.KEYSTORE_FILE_NAME);
			builder.exportVar(CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR, details.getKeystorePassword());
		}

		if (details.getOpenFilesLimit() != null) {
			builder.exportVar(CloudifyConstants.CLOUDIFY_OPEN_FILES_LIMIT, details.getOpenFilesLimit());
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
	 * Normalizes the path and returns a standard windows absolute path.
	 * 
	 * @param remoteDirectory
	 *            The remote path.
	 * @return a normalized absolute windows path.
	 */
	public static String normalizeLocalAbsolutePath(final String remoteDirectory) {
		if (remoteDirectory.startsWith("/") && remoteDirectory.indexOf("$") == 2) {
			// '$' is a special char.
			final String quoteReplacement = Matcher.quoteReplacement("$");
			return remoteDirectory.replaceFirst(quoteReplacement, ":").replaceFirst("/", "");
		}
		return remoteDirectory;
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

	/***********
	 * Prepares the final environment output. Should only be called after all other values have been set.
	 * 
	 * @return this instance.
	 */
	public EnvironmentFileBuilder build() {
		// add only environment vars that were not appended to existing vars.
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
