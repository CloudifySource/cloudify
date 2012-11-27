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
package org.cloudifysource.shell;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.Ansi.Erase;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.j_spaces.kernel.Environment;
import com.j_spaces.kernel.PlatformVersion;

/**
 * @author rafi, barakm
 * @since 2.0.0
 *        <p/>
 *        This class includes different utilities used across the CLI.
 */
public final class ShellUtils {

	private static final Logger logger = Logger.getLogger(ShellUtils.class.getName());

	private static final char WIN_RETURN_CHAR = '\r';
	private static final char LINUX_RETURN_CHAR = '\n';

	private static final long TWO_WEEKS_IN_MILLIS = 86400000L * 14L;
	private static final File VERSION_CHECK_FILE =
			new File(System.getProperty("user.home") + "/.karaf/lastVersionCheckTimestamp");

	private static final int VERSION_CHECK_READ_TIMEOUT = 5000;

	private static final String BOLD_ANSI_CHAR_SEQUENCE = "\u001B[1m";
	private static final char FIRST_ESC_CHAR = 27;
	private static final char SECOND_ESC_CHAR = '[';
	private static final char COMMAND_CHAR = 'm';
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	private static volatile ResourceBundle defaultMessageBundle;

	private ShellUtils() {

	}

	/**
	 * returns the message as it appears in the message bundle.
	 * 
	 * @param msgName the message key as it is defined in the message bundle.
	 * @param arguments the message arguments
	 * @return the formatted message according to the message key.
	 */
	public static String getFormattedMessage(final String msgName, final Object... arguments) {

		final String message = getMessageBundle().getString(msgName);
		if (message == null) {
			logger.warning("Missing resource in messages resource bundle: " + msgName);
			return msgName;
		}
		try {
			return MessageFormat.format(message, arguments);
		} catch (final IllegalArgumentException e) {
			logger.fine("Failed to format message: " + msgName + " with format: "
					+ message + " and arguments: " + Arrays.toString(arguments));
			return msgName;
		}
	}

	/**
	 * 
	 * @param session the command session.
	 * @param messageKey the message key.
	 * @return true if user hits 'y' OR 'yes' else returns false
	 * @throws IOException indicates a failure while accessing the session's stdout.
	 */
	public static boolean promptUser(final CommandSession session, final String messageKey)
			throws IOException {
		return promptUser(session, messageKey, EMPTY_OBJECT_ARRAY);

	}

	/**
	 * prompts the user with the given question.
	 * 
	 * @param session the command session.
	 * @param messageKey the message key.
	 * @param messageArgs the message arguments.
	 * @return true if user hits 'y' OR 'yes' else returns false
	 * @throws IOException Indicates a failure while accessing the session's stdout.
	 */
	public static boolean promptUser(final CommandSession session, final String messageKey,
			final Object... messageArgs)
			throws IOException {
		if ((Boolean) session.get(Constants.INTERACTIVE_MODE)) {
			session.getConsole().print(Ansi.ansi().eraseLine(Erase.ALL));
			final String confirmationQuestion = ShellUtils.getFormattedMessage(messageKey, messageArgs);
			session.getConsole().print(confirmationQuestion + " ");
			session.getConsole().flush();
			char responseChar = '\0';
			StringBuilder responseBuffer = new StringBuilder();
			while (true) {
				responseChar = (char) session.getKeyboard().read();
				if (responseChar == '\u007F') { // backspace
					if (responseBuffer.length() > 0) {
						responseBuffer.deleteCharAt(responseBuffer.length() - 1);
						session.getConsole().print(Ansi.ansi().cursorLeft(1).eraseLine());
					}
				} else if (responseChar == WIN_RETURN_CHAR || responseChar == LINUX_RETURN_CHAR) {
					session.getConsole().println();
					break;
				} else {
					session.getConsole().print(responseChar);
					responseBuffer.append(responseChar);
				}
				session.getConsole().flush();
			}
			String responseStr = responseBuffer.toString().trim();
			return "y".equalsIgnoreCase(responseStr) || "yes".equalsIgnoreCase(responseStr);
		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}

	/**
	 * Gets the types of the managed components as a collection of lower case Strings.
	 * 
	 * @return The types of the managed components as a collection of lower case Strings.
	 */
	public static Collection<String> getComponentTypesAsLowerCaseStringCollection() {
		final ComponentType[] componentTypes = ComponentType.values();
		final ArrayList<String> componentTypesAsString = new ArrayList<String>(componentTypes.length);
		for (final ComponentType type : componentTypes) {
			componentTypesAsString.add(type.toString().toLowerCase());
		}
		return componentTypesAsString;
	}

	/**
	 * Gets the given message formatted to be displayed in the specified color.
	 * 
	 * @param message The text message
	 * @param color The color the message should be displayed in
	 * @return A formatted message text
	 */
	public static String getColorMessage(final String message, final Color color) {
		String formattedMessage = Ansi.ansi().fg(color).a(message).toString();
		return formattedMessage + FIRST_ESC_CHAR + SECOND_ESC_CHAR + '0' + COMMAND_CHAR;
	}

	/**
	 * Gets the given message formatted to be displayed in bold characters.
	 * 
	 * @param message The text message
	 * @return A formatted message text
	 */
	public static String getBoldMessage(final String message) {
		return BOLD_ANSI_CHAR_SEQUENCE + message + FIRST_ESC_CHAR
				+ SECOND_ESC_CHAR + '0' + COMMAND_CHAR;
	}

	/**
	 * Converts a comma-delimited string of instance IDs to a set of Integers.
	 * 
	 * @param componentInstanceIDs a comma-delimited string of instance IDs
	 * @return instance IDs as a set of Integers
	 */
	public static Set<Integer> delimitedStringToSet(final String componentInstanceIDs) {
		final String[] delimited = componentInstanceIDs.split(",");
		final Set<Integer> intSet = new HashSet<Integer>();
		for (final String str : delimited) {
			intSet.add(Integer.valueOf(str));
		}
		return intSet;
	}

	/**
	 * Gets the recipes map from the session.
	 * 
	 * @param session The command session to query
	 * @return The recipes map
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, File> getRecipes(final CommandSession session) {
		return (Map<String, File>) session.get(Constants.RECIPES);
	}

	/**
	 * Gets the built-in messages bundle, with the default locale.
	 * 
	 * @return The messages bundle
	 */
	public static ResourceBundle getMessageBundle() {

		if (defaultMessageBundle == null) {
			defaultMessageBundle = ResourceBundle.getBundle(
					"MessagesBundle", Locale.getDefault());
		}
		return defaultMessageBundle;

	}

	/**
	 * Converts the named component to a {@link ComponentType}.
	 * 
	 * @param lowerCaseComponentName The component name, in lower case
	 * @return The matching {@link ComponentType}
	 * @throws CLIException Reporting failure to find a matching {@link ComponentType} for the given name
	 */
	public static ComponentType componentTypeFromLowerCaseComponentName(final String lowerCaseComponentName)
			throws CLIException {
		try {
			return ComponentType.valueOf(lowerCaseComponentName.toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new CLIStatusException("unknown_component_type", lowerCaseComponentName, e);
		}
	}

	/**
	 * Calculates how many milliseconds ahead is the specified target time. If it has passed already, throws a
	 * {@link TimeoutException} with the given error message.
	 * 
	 * @param errorMessage The error message of the {@link TimeoutException}, if thrown
	 * @param end The target time, formatted in milliseconds
	 * @return The number of milliseconds ahead, before the target time is reached
	 * @throws TimeoutException Indicating the target time is in the past
	 */
	public static long millisUntil(final String errorMessage, final long end)
			throws TimeoutException {
		final long millisUntilEnd = end - System.currentTimeMillis();
		if (millisUntilEnd < 0) {
			throw new TimeoutException(errorMessage);
		}
		return millisUntilEnd;
	}

	/**
	 * Gets an "expected execution time" formatted message, with the current time in this format: HH:mm.
	 * 
	 * @return a formatted "expected execution time" message
	 */
	public static String getExpectedExecutionTimeMessage() {
		final String currentTime = new SimpleDateFormat("HH:mm").format(new Date());
		return MessageFormat.format(
				getMessageBundle().getString(
						"expected_execution_time"), currentTime);
	}

	/**
	 * Gets the CLI directory.
	 * 
	 * @return the CLI directory
	 */
	public static File getCliDirectory() {
		return new File(Environment.getHomeDirectory(), "/tools/cli");
	}

	/**
	 * Verifies the given value is not null. If it is - throws an IllegalArgumentException with the message: <name>
	 * cannot be null.
	 * 
	 * @param name The name to be used in the exception, if thrown
	 * @param value The value to verify
	 */
	public static void checkNotNull(final String name, final Object value) {
		if (value == null) {
			throw new IllegalArgumentException(name + " cannot be null");
		}
	}

	/**
	 * Reads the properties from the specified file, and loads them into a {@link Properties} object.
	 * 
	 * @param propertiesFile The file to read properties from
	 * @return A populated properties object
	 * @throws IOException Thrown if the specified file is not found or accessed appropriately
	 */
	public static Properties loadProperties(final File propertiesFile)
			throws IOException {
		final Properties properties = new Properties();
		final FileInputStream fis = new FileInputStream(propertiesFile);
		try {
			properties.load(fis);
		} finally {
			fis.close();
		}
		return properties;
	}

	/**
	 * Checks if the operating system on this machine is Windows.
	 * 
	 * @return True - if using Windows, False - otherwise.
	 */
	public static boolean isWindows() {
		final String os = System.getProperty(
				"os.name").toLowerCase();
		return os.indexOf("win") >= 0;
	}

	/**
	 * returns true if the last version check was done more than two weeks ago.
	 * 
	 * @param session the command session.
	 * @return true if a version check is required else returns false.
	 */
	public static boolean shouldDoVersionCheck(final CommandSession session) {
		final boolean interactive = (Boolean) session.get(Constants.INTERACTIVE_MODE);
		if (!interactive) {
			return false;
		}

		long lastAskedTS = getLastTimeAskedAboutVersionCheck();
		// check only if checked over a two weeks ago and user agrees
		try {
			if (lastAskedTS <= (System.currentTimeMillis() - TWO_WEEKS_IN_MILLIS)) {
				boolean userConfirms = ShellUtils.promptUser(session, "version_check_confirmation");
				registerVersionCheck();
				return userConfirms;
			}
		} catch (IOException e) {
			logger.log(Level.FINE, "Failed to prompt user", e);
		}
		return false;
	}

	/**
	 * Checks if the latest version is used.
	 * 
	 * @param session the command session.
	 */
	public static void doVersionCheck(final CommandSession session) {
		String currentBuildStr = PlatformVersion.getBuildNumber();
		if (currentBuildStr.contains("-")) {
			currentBuildStr = currentBuildStr.substring(0, currentBuildStr.indexOf("-"));
		}
		int currentVersion = Integer.parseInt(currentBuildStr);
		int latestBuild = getLatestBuildNumber(currentVersion);
		String message;
		if (latestBuild == -1) {
			message = ShellUtils.getFormattedMessage("could_not_get_version");
		} else if (latestBuild > currentVersion) {
			message = ShellUtils.getFormattedMessage("newer_version_exists");
		} else {
			message = ShellUtils.getFormattedMessage("version_up_to_date");
		}
		session.getConsole().println(message);
		session.getConsole().println();
	}

	/**
	 * returns the latest cloudify version.
	 * 
	 * @param currentVersion the current version.
	 * @return the latest cloudify version.
	 */
	public static int getLatestBuildNumber(final int currentVersion) {
		try {
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setReadTimeout(VERSION_CHECK_READ_TIMEOUT);
			RestTemplate template = new RestTemplate(requestFactory);
			String versionStr = template.getForObject(
					"http://www.gigaspaces.com/downloadgen/latest-cloudify-version?build=" + currentVersion,
					String.class);
			logger.fine("Latest cloudify version is " + versionStr);
			return Integer.parseInt(versionStr);
		} catch (RestClientException e) {
			logger.log(Level.FINE, "Could not get version from server", e);
			return -1;
		} catch (NumberFormatException e) {
			logger.fine("Get version response is not a number");
			return -1;
		}

	}

	/**
	 * Returns the last time a version check was performed.
	 * 
	 * @return the last time a version check was performed.
	 */
	public static long getLastTimeAskedAboutVersionCheck() {
		long lastVersionCheckTS = 0;
		if (VERSION_CHECK_FILE.exists()) {
			DataInputStream dis = null;
			try {
				dis = new DataInputStream(new FileInputStream(VERSION_CHECK_FILE));
				lastVersionCheckTS = dis.readLong();
			} catch (IOException e) {
				logger.log(Level.FINE, "failed to read last checked version timestamp file", e);
			} finally {
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return lastVersionCheckTS;
	}

	/**
	 * updates the file that contains the last version check time.
	 */
	public static void registerVersionCheck() {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(VERSION_CHECK_FILE));
			dos.writeLong(System.currentTimeMillis());
		} catch (IOException e) {
			logger.log(Level.FINE, "failed to write last checked version timestamp file", e);
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * Checks if the passed security profile uses a secure connection (SSL).
	 * @param springSecurityProfile The name of the security profile
	 * @return true - if the profile indicates SSL is used, false otherwise.
	 */
	public static boolean isSecureConnection(final String springSecurityProfile) {
		return CloudifyConstants.SPRING_PROFILE_SSL.equalsIgnoreCase(springSecurityProfile);
	}
	
	/**
	 * Returns the name of the protocol used for communication with the rest server.
	 * If the security is secure (SSL) returns "https", otherwise returns "http".
	 * @param springSecurityProfile The name of the security profile
	 * @return "https" if this is a secure connection, "http" otherwise.
	 */
	public static String getRestProtocol(final String springSecurityProfile) {
		return getRestProtocol(isSecureConnection(springSecurityProfile));
	}
	
	/**
	 * Returns the name of the protocol used for communication with the rest server.
	 * If the security is secure (SSL) returns "https", otherwise returns "http".
	 * @param isSecureConnection Indicates whether SSL is used or not.
	 * @return "https" if this is a secure connection, "http" otherwise.
	 */
	public static String getRestProtocol(final boolean isSecureConnection) {
		if (isSecureConnection) {
			return "https";
		} else {
			return "http";
		}
	}
	
	/**
	 * Returns the port used for communication with the rest server.
	 * If the security is secure (SSL) returns "8443", otherwise returns "8100".
	 * @param isSecureConnection Indicates whether SSL is used or not.
	 * @return 8443 if this is a secure connection, 8100 otherwise.
	 */
	public static int getRestPort(final String springSecurityProfile) {
		return getRestPort(isSecureConnection(springSecurityProfile));
	}
	
	/**
	 * Returns the port used for communication with the rest server.
	 * If the security is secure (SSL) returns "8443", otherwise returns "8100".
	 * @param isSecureConnection Indicates whether SSL is used or not.
	 * @return 8443 if this is a secure connection, 8100 otherwise.
	 */
	public static int getRestPort(final boolean isSecureConnection) {
		if (isSecureConnection) {
			return CloudifyConstants.SECURE_REST_PORT;
		} else {
			return CloudifyConstants.DEFAULT_REST_PORT;
		}
	}
	
	/**
	 * Returns the port used for communication with the rest server.
	 * If the security is secure (SSL) returns "8443", otherwise returns "8100".
	 * @param isSecureConnection Indicates whether SSL is used or not.
	 * @return "8443" if this is a secure connection, "8100" otherwise.
	 */
	public static String getRestPortAsString(final boolean isSecureConnection) {
		return Integer.toString(getRestPort(isSecureConnection));
	}
	
	public static String getFormattedRestUrl(String url, String springSecurityProfile) throws MalformedURLException {
		return getFormattedRestUrl(url, isSecureConnection(springSecurityProfile));		
	}
	
	public static String getFormattedRestUrl(String url, boolean isSecureConnection) throws MalformedURLException {
		String formattedURL = url;
		if (!formattedURL.endsWith("/")) {
			formattedURL = formattedURL + '/';
		}
		
		String protocolPrefix = ShellUtils.getRestProtocol(isSecureConnection) + "://";
		if (!formattedURL.startsWith("http://") && !formattedURL.startsWith("https://")) {
			formattedURL = protocolPrefix + formattedURL;
		}

		URL urlObj;
		urlObj = new URL(formattedURL);
		if (urlObj.getPort() == -1) {
			final StringBuilder urlSB = new StringBuilder(formattedURL);
			final int portIndex = formattedURL.indexOf("/", protocolPrefix.length());
			urlSB.insert(portIndex, ':' + ShellUtils.getRestPortAsString(isSecureConnection));
			formattedURL = urlSB.toString();
			urlObj = new URL(formattedURL);
		}
		
		return formattedURL;
	}
	
}
