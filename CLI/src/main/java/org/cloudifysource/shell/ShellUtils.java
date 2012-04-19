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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        This class includes different utilities used across the CLI.
 */
public final class ShellUtils {

	private static final char FIRST_ESC_CHAR = 27;
	private static final char SECOND_ESC_CHAR = '[';
	private static final char COMMAND_CHAR = 'm';
	private static volatile ResourceBundle defaultMessageBundle;

	private ShellUtils() {

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
	 * @param message
	 *            The text message
	 * @param color
	 *            The color the message should be displayed in
	 * @return A formatted message text
	 */
	public static String getColorMessage(final String message, final Color color) {
		String formattedMessage = message;
		formattedMessage = Ansi.ansi().fg(
				color).a(
				message).toString();
		return formattedMessage + FIRST_ESC_CHAR + SECOND_ESC_CHAR + '0' + COMMAND_CHAR;
	}

	/**
	 * Converts a comma-delimited string of instance IDs to a set of Integers.
	 * 
	 * @param componentInstanceIDs
	 *            a comma-delimited string of instance IDs
	 * @return instance IDs as a set of Integers
	 */
	public static Set<Integer> delimitedStringToSet(final String componentInstanceIDs)
			{
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
	 * @param session
	 *            The command session to query
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
	 * @param lowerCaseComponentName
	 *            The component name, in lower case
	 * @return The matching {@link ComponentType}
	 * @throws CLIException
	 *             Reporting failure to find a matching {@link ComponentType} for the given name
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
	 * @param errorMessage
	 *            The error message of the {@link TimeoutException}, if thrown
	 * @param end
	 *            The target time, formatted in milliseconds
	 * @return The number of milliseconds ahead, before the target time is reached
	 * @throws TimeoutException
	 *             Indicating the target time is in the past
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
	 * @param name
	 *            The name to be used in the exception, if thrown
	 * @param value
	 *            The value to verify
	 */
	public static void checkNotNull(final String name, final Object value) {
		if (value == null) {
			throw new IllegalArgumentException(name + " cannot be null");
		}
	}

	/**
	 * Reads the properties from the specified file, and loads them into a {@link Properties} object.
	 * 
	 * @param propertiesFile
	 *            The file to read properties from
	 * @return A populated properties object
	 * @throws IOException
	 *             Thrown if the specified file is not found or accessed appropriately
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
}
