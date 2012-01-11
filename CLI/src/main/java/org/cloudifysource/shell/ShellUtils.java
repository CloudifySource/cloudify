/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
 * @author rafi
 * @since 8.0.3
 */
public class ShellUtils {

    private static final char FIRST_ESC_CHAR = 27;
    private static char SECOND_ESC_CHAR = '[';
    private static char COMMAND_CHAR = 'm';
    
    public static Collection<String> getComponentTypesAsLowerCaseStringCollection() {
        final ComponentType[] componentTypes = ComponentType.values();
        final ArrayList<String> componentTypesAsString = new ArrayList<String>(
                componentTypes.length);
        for (final ComponentType type : componentTypes) {
            componentTypesAsString.add(type.toString().toLowerCase());
        }
        return componentTypesAsString;
    }
    
    public static String getColorMessage(String message, Color color){
		message = Ansi.ansi().fg(color).a(message).toString();
		return message + FIRST_ESC_CHAR + SECOND_ESC_CHAR + '0' + COMMAND_CHAR;
    }

    public static Set<Integer> delimitedStringToSet(
            final String componentInstanceIDs) throws NumberFormatException {
        final String[] delimited = componentInstanceIDs.split(",");
        final Set<Integer> intSet = new HashSet<Integer>();
        for (final String str : delimited) {
            intSet.add(Integer.parseInt(str));
        }
        return intSet;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, File> getRecipes(final CommandSession session) {
        return (Map<String, File>) session.get(Constants.RECIPES);
    }

    public static ResourceBundle getMessageBundle() {
        return ResourceBundle.getBundle("MessagesBundle", Locale.getDefault());
    }

    public static ComponentType componentTypeFromLowerCaseComponentName(
            final String lowerCaseComponentName) throws CLIException {
        try {
            return ComponentType.valueOf(lowerCaseComponentName.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new CLIStatusException("unknown_component_type",
                    lowerCaseComponentName);
        }
    }

    public static long millisUntil(String errorMessage, long end)
            throws TimeoutException {
        long millisUntilEnd = end - System.currentTimeMillis();
        if (millisUntilEnd < 0) {
            throw new TimeoutException(errorMessage);
        }
        return millisUntilEnd;
    }

    public static String getExpectedExecutionTimeMessage() {
        String currentTime = new SimpleDateFormat("HH:MM").format(new Date());
        return MessageFormat.format(
                getMessageBundle().getString("expected_execution_time"),
                currentTime);
    }

    public static File getCliDirectory() {
        return new File(Environment.getHomeDirectory(), "/tools/cli");
    }

    public static void checkNotNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public static Properties loadProperties(File propertiesFile)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(propertiesFile);
        try {
            properties.load(fis);
        } finally {
            fis.close();
        }
        return properties;
    }   
    
    public static boolean isWindows() {
        final String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }
}
