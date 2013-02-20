/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.usm.launcher;

import groovy.lang.Closure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.entry.ClosureExecutableEntry;
import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.entry.ExecutableDSLEntryType;
import org.cloudifysource.dsl.entry.ListExecutableEntry;
import org.cloudifysource.dsl.entry.StringExecutableEntry;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.dsl.ServiceConfiguration;
import org.hyperic.sigar.Sigar;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.springframework.beans.factory.annotation.Autowired;

import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;
import com.j_spaces.kernel.PlatformVersion;

/*************
 * The default process launcher implementation, used by the USM to launch external processes. Includes OS specific code
 * to modify a command line according to standard command line conventions.
 *
 * @author barakme
 *
 */
public class DefaultProcessLauncher implements ProcessLauncher, ClusterInfoAware {

	private static final int POST_SYNC_PROCESS_SLEEP_INTERVAL = 200;
	private static final String LINUX_EXECUTE_PREFIX = "./";
	private static final String[] WINDOWS_BATCH_FILE_PREFIX_PARAMS = { "cmd.exe", "/c " };
	private List<String> groovyCommandLinePrefixParams;
	// last command line to be executed
	private List<String> commandLine;
	private final Sigar sigar = SigarHolder.getSigar();
	private ClusterInfo clusterInfo;
	private String groovyEnvironmentClassPath;

	@Autowired
	private ServiceConfiguration configutaion;

	private List<String> switchFirstPartOfCommandLine(final List<String> originalCommandLine, final String newPart) {

		final List<String> newCommand = new LinkedList<String>();
		newCommand.add(newPart);
		newCommand.addAll(originalCommandLine.subList(1,
				originalCommandLine.size()));
		return newCommand;

	}

	private List<String> createWindowsCommandLineFromLinux(final List<String> linuxCommandLine, final File puWorkDir) {

		final AlternativeExecutableFileNameFilter filter = new AlternativeExecutableFileNameFilter() {

			private String prefix;

			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(prefix) && (name.endsWith(".bat") || name.endsWith(".exe"));

			}

			@Override
			public void setPrefix(final String prefix) {
				this.prefix = prefix;

			}
		};

		return createCommandLineFromAlternativeOS(puWorkDir,
				linuxCommandLine,
				filter);

	}

	/**********
	 * An interface for a Filename filter implementation that looks for alternative executables.
	 *
	 * @author barakme
	 *
	 */
	private interface AlternativeExecutableFileNameFilter extends FilenameFilter {

		void setPrefix(String prefix);
	}

	private List<String> createCommandLineFromAlternativeOS(final File puWorkDir,
			final List<String> alternateCommandLine, final AlternativeExecutableFileNameFilter fileNameFilter) {

		if (alternateCommandLine == null || alternateCommandLine.isEmpty()) {
			return null;
		}

		final String executable = alternateCommandLine.get(0);

		// groovy files are executable on all OS
		if (executable.endsWith(".groovy")) {
			return alternateCommandLine;
		}

		final File executableFile = getFileFromRelativeOrAbsolutePath(puWorkDir,
				executable);
		if (executableFile == null) {
			logger.warning("Could not find an executable file: " + executable + "in working directory " + puWorkDir
					+ " in command line: " + alternateCommandLine + ". Alternate command line can't be created.");
			return null;
		}

		final String prefix = getFilePrefix(executableFile);

		final File parentDir = executableFile.getParentFile();

		fileNameFilter.setPrefix(prefix);
		final File[] files = parentDir.listFiles(fileNameFilter);

		if (files.length == 0) {
			return null;
		}

		logger.info("Found candidates for command line to replace " + executable + ". Candidates are: "
				+ Arrays.toString(files));

		final List<String> modifiedCommandLine = switchFirstPartOfCommandLine(alternateCommandLine,
				files[0].getName());

		return modifiedCommandLine;

	}

	private String getFilePrefix(final File executableFile) {
		String prefixToSearch = executableFile.getName();
		final int index = prefixToSearch.lastIndexOf('.');
		if (index >= 0) {
			prefixToSearch = prefixToSearch.substring(0,
					index);
		}
		return prefixToSearch;
	}

	private List<String> createLinuxCommandLineFromWindows(final List<String> windowsCommandLine,
			final File puWorkDir) {
		final AlternativeExecutableFileNameFilter filter = new AlternativeExecutableFileNameFilter() {

			private String prefix;

			@Override
			public boolean accept(final File dir, final String name) {
				return name.equals(prefix) || name.startsWith(prefix) && name.endsWith(".sh");
			}

			@Override
			public void setPrefix(final String prefix) {
				this.prefix = prefix;

			}
		};

		return createCommandLineFromAlternativeOS(puWorkDir,
				windowsCommandLine,
				filter);

	}

	private void modifyGroovyCommandLine(final List<String> commandLineParams, final File workingDir)
			throws USMException {

		try {
			initGroovyCommandLine(workingDir);
		} catch (final FileNotFoundException e) {
			throw new USMException("Failed to set up groovy command line", e);
		}

		for (int i = 0; i < groovyCommandLinePrefixParams.size(); i++) {
			commandLineParams.add(i,
					groovyCommandLinePrefixParams.get(i));
		}
	}

	private void addJarsFromDirectoryToList(final File dir, final List<File> list) {
		final File[] jars = getJarFilesFromDir(dir);
		if (jars != null) { // possibe if directory does not exist
			list.addAll(Arrays.asList(jars));
		}
	}

	private List<File> getJarFilesForGroovyClasspath(final File gsHome, final File workingDir) {
		final List<File> list = new LinkedList<File>();

		// jar files in PU's lib dir
		addJarsFromDirectoryToList(new File(workingDir.getParentFile(), "lib"),
				list);

		// <GS_HOME>/lib/required
		addJarsFromDirectoryToList(new File(gsHome, "lib/required"),
				list);

		// <GS_HOME>/lib/platform/usm
		addJarsFromDirectoryToList(new File(gsHome, "lib/platform/usm"),
				list);

		addJarsFromDirectoryToList(new File(gsHome, "lib/platform/cloudify"),
				list);

		addJarsFromDirectoryToList(new File(gsHome, "lib/platform/sigar"),
				list);

		return list;

	}

	private File[] getJarFilesFromDir(final File dir) {
		final File[] jars = dir.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File pathname) {
				return pathname.getName().endsWith(".jar") && pathname.isFile();
			}
		});
		return jars;
	}

	private void initGroovyCommandLine(final File workingDir)
			throws FileNotFoundException, USMException {
		if (this.groovyCommandLinePrefixParams != null) {
			return;
		}
		final String home = Environment.getHomeDirectory();

		final File homeDir = new File(home);
		final String groovyPath = createGroovyPath(homeDir);
		final StringBuilder sb = new StringBuilder();

		final List<File> jars = getJarFilesForGroovyClasspath(homeDir,
				workingDir);

		if (jars != null) {

			for (final File jar : jars) {
				sb.append(jar.getAbsolutePath()).append(File.pathSeparator);
			}

		}

		final ArrayList<String> groovyCommandParams = new ArrayList<String>();
		groovyCommandParams.add(groovyPath);

		// pass values as system props to the jvm, as required by XAP
		final List<String> envVarsList = new ArrayList<String>();
		envVarsList.add("LOOKUP_LOCATORS_PROP");
		envVarsList.add("LOOKUP_GROUPS_PROP");
		envVarsList.add("RMI_OPTIONS");
		envVarsList.add("GS_LOGGING_CONFIG_FILE_PROP");

		groovyCommandParams.addAll(convertEnvVarsToSysPropsList(envVarsList));

		if (ServiceUtils.isWindows()) {
			modifyWindowsCommandLine(groovyCommandParams, workingDir);
		}

		String classPathEnv = System.getenv("CLASSPATH");
		if (classPathEnv == null) {
			classPathEnv = "";
		}
		classPathEnv = classPathEnv + File.pathSeparator + sb.toString();

		// We use the classpath env variable, as the full classpath may be too
		// long for the command line
		// limit imposed by the operating system.
		logger.info("Setting ClassPath environment variable for child processes to: " + classPathEnv);
		this.groovyEnvironmentClassPath = classPathEnv;
		// if ((jars != null) && (jars.size() > 0)) {
		// groovyCommandParams.add("-cp");
		// groovyCommandParams.add(sb.toString());
		// }

		logger.info("Setting groovyCommandParams to: " + groovyCommandParams);
		this.groovyCommandLinePrefixParams = groovyCommandParams;
	}

	private String createGroovyPath(final File homeDir)
			throws FileNotFoundException, USMException {
		final File toolsDir = new File(homeDir, "tools");
		final File groovyDir = new File(toolsDir, "groovy");
		final File binDir = new File(groovyDir, "bin");
		File groovyFile = null;
		if (ServiceUtils.isWindows()) {
			groovyFile = new File(binDir, "groovy.bat");
		} else {
			groovyFile = new File(binDir, "groovy");
		}

		if (!groovyFile.exists()) {
			throw new FileNotFoundException("Could not find groovy executable: " + groovyFile.getAbsolutePath());
		}

		if (ServiceUtils.isLinuxOrUnix()) {
			USMUtils.markFileAsExecutable(sigar,
					groovyFile);
		}

		return groovyFile.getAbsolutePath();

	}

	private void modifyOSCommandLine(final List<String> commandLineParams, final File puWorkDir)
			throws USMException {
		final String runParam = commandLineParams.get(0);

		if (ServiceUtils.isWindows()) {
			modifyWindowsCommandLine(commandLineParams,
					puWorkDir);
		} else {
			markLinuxTargetAsExecutable(runParam,
					puWorkDir);
			modifyLinuxCommandLine(commandLineParams,
					puWorkDir);

		}

	}

	// Add "./" to command line, if not present and file is in ext dir
	private void modifyLinuxCommandLine(final List<String> commandLineParams, final File puWorkDir) {
		String executeScriptName = commandLineParams.get(0);
		final File executeFile = new File(puWorkDir, executeScriptName);
		final File parent = executeFile.getParentFile();
		if (parent != null && parent.equals(puWorkDir)) {
			if (executeScriptName.endsWith(".sh") && !executeScriptName.startsWith(LINUX_EXECUTE_PREFIX)) {
				commandLineParams.remove(0);
				executeScriptName = LINUX_EXECUTE_PREFIX + executeScriptName;
				commandLineParams.add(0,
						executeScriptName);
			}
		}

		// LinkedList<String> linkedList =
		// (LinkedList<String>)commandLineParams;
		// linkedList.addAll(0, Arrays.asList("sh", "-c", "\""));

	}

	private void modifyWindowsCommandLine(final List<String> commandLineParams, final File workingDir) {
		final String firstParam = commandLineParams.get(0);
		if (firstParam.endsWith(".bat") || firstParam.endsWith(".cmd")) {
			for (int i = 0; i < WINDOWS_BATCH_FILE_PREFIX_PARAMS.length; i++) {
				commandLineParams.add(i,
						WINDOWS_BATCH_FILE_PREFIX_PARAMS[i]);
			}
		}

		// if the file does not exist, this is probably an operating system
		// command
		File file = new File(firstParam);
		if (!file.isAbsolute()) {
			file = new File(workingDir, firstParam);
		}

		if (!file.exists()) {
			// this is not an executable file, so add the cmd interpreter
			// prefix
			for (int i = 0; i < WINDOWS_BATCH_FILE_PREFIX_PARAMS.length; i++) {
				commandLineParams.add(i,
						WINDOWS_BATCH_FILE_PREFIX_PARAMS[i]);
			}
		}

		// remove quotes
		final ListIterator<String> commandIterator = commandLineParams.listIterator();
		while (commandIterator.hasNext()) {
			final String param = commandIterator.next();
			commandIterator.remove();
			commandIterator.add(StringUtils.replace(param, "\"", ""));
		}

	}

	private File getFileFromRelativeOrAbsolutePath(final File puWorkDir, final String fileName) {
		File file = new File(puWorkDir, fileName);
		if (file.exists()) {
			return file;
		}
		file = new File(fileName);
		if (file.exists()) {
			return file;
		}
		return null;
	}

	private void markLinuxTargetAsExecutable(final String originalCommandLineRunParam, final File puWorkDir)
			throws USMException {

		logger.info("Setting File as executable: " + originalCommandLineRunParam);
		final File file = getFileFromRelativeOrAbsolutePath(puWorkDir,
				originalCommandLineRunParam);
		if (file != null) {
			logger.info("File: " + file + " will be marked as executable");
			USMUtils.markFileAsExecutable(sigar,
					file);
		}

	}

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DefaultProcessLauncher.class
			.getName());

	private List<String> convertCommandLineStringToParts(final String commandLine) {
		final List<String> list = new LinkedList<String>();
		final String[] parts = commandLine.split(" ");
		Collections.addAll(list, parts);
		return list;
	}

	private List<String> getCommandLineFromArgument(final ExecutableDSLEntry arg, final File workDir,
			final List<String> params) {
		if (arg.getEntryType() == ExecutableDSLEntryType.STRING) {
			// Split the command into parts
			final List<String> commandLineStringInParts =
					convertCommandLineStringToParts(((StringExecutableEntry) arg).getCommand());
			// Add the custom command parameters as args to the split commands
			// array
			if (params != null) {
				commandLineStringInParts.addAll(params);
			}

			return commandLineStringInParts;
		}

		if (arg.getEntryType() == ExecutableDSLEntryType.MAP) {
			@SuppressWarnings("unchecked")
			final Map<String, ExecutableDSLEntry> map = (Map<String, ExecutableDSLEntry>) arg;

			final String os = System.getProperty("os.name");
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Looking for command line for Operating System Name: " + os);
			}

			final List<String> cmdLineList = lookUpCommandLineForOS(map,
					os);
			if (cmdLineList != null) {
				return cmdLineList;
			}

			logger.severe("Could not find a matching operating system expression for Operating System: " + os);
			logger.severe("Attempting alternative command line: " + os);

			final List<String> alternativeCommandLine = createAlternativeCommandLine(map,
					workDir);
			if (alternativeCommandLine != null) {
				return alternativeCommandLine;
			}
			logger.severe("Could not create alternative command line: " + os);

		}

		if (arg.getEntryType() == ExecutableDSLEntryType.LIST) {
			final List<?> originalList = ((ListExecutableEntry) arg).getCommand();

			final List<String> resultList = new ArrayList<String>(originalList.size());
			for (final Object elem : originalList) {
				resultList.add(elem.toString());
			}
			return resultList;
		}
		throw new IllegalArgumentException("Could not find command line for argument " + arg + "!");

	}

	private List<String> getCommandLineFromValue(final ExecutableDSLEntry value) {
		if (value.getEntryType() == ExecutableDSLEntryType.STRING) {
			return convertCommandLineStringToParts(((StringExecutableEntry) value).getCommand());
		} else if (value.getEntryType() == ExecutableDSLEntryType.LIST) {
			final List<String> result = ((ListExecutableEntry) value).getCommand();
			return result;
		} else {
			throw new IllegalArgumentException("The value: " + value
					+ " could not be converted to a command line. Only a String, or a List of Strings may be used");
		}
	}

	private List<String> lookUpCommandLineForOS(final Map<String, ExecutableDSLEntry> map, final String os) {
		final Set<Entry<String, ExecutableDSLEntry>> entries = map.entrySet();
		for (final Entry<String, ExecutableDSLEntry> entry : entries) {
			final String key = entry.getKey();
			final ExecutableDSLEntry value = entry.getValue();
			try {
				if (Pattern.matches(key,
						os)) {
					return getCommandLineFromValue(value);

				}
			} catch (final PatternSyntaxException pse) {
				logger.log(Level.WARNING,
						"Opeating System regular expression pattern: " + key
								+ " cannot be compiled. It will me compared using basic string matching.",
						pse);
				if (key.equals(os)) {
					return getCommandLineFromValue(value);
				}
			}

		}
		return null;
	}

	private List<String> createAlternativeCommandLine(final Map<String, ExecutableDSLEntry> map, final File workDir) {
		if (ServiceUtils.isWindows()) {
			List<String> otherCommandLine = null;

			if (map.entrySet().size() > 1) {
				otherCommandLine = lookUpCommandLineForOS(map, "Linux");
			}

			if (otherCommandLine == null) {
				otherCommandLine = getCommandLineFromValue(map.values().iterator().next());
			}

			return createWindowsCommandLineFromLinux(otherCommandLine,
					workDir);
		}
		List<String> otherCommandLine = null;

		if (map.entrySet().size() > 1) {
			otherCommandLine = lookUpCommandLineForOS(map, "Windows");
		}

		if (otherCommandLine == null) {
			otherCommandLine = getCommandLineFromValue(map.values().iterator().next());
		}

		return createLinuxCommandLineFromWindows(otherCommandLine, workDir);
	}

	@Override
	public Process launchProcessAsync(final ExecutableDSLEntry arg, final File workingDir, final int retries,
			final boolean redirectErrorStream, final List<String> params)
			throws USMException {
		return launchAsync(arg,
				workingDir,
				retries,
				redirectErrorStream,
				null,
				null,
				params);
	}

	private Process launchAsync(final ExecutableDSLEntry arg, final File workingDir, final int retries,
			final boolean redirectErrorStream, final File outputFile, final File errorFile, final List<String> params)
			throws USMException {
		if (arg.getEntryType() == ExecutableDSLEntryType.CLOSURE) {
			// in process execution of a closure
			return launchAsyncFromClosure(((ClosureExecutableEntry) arg).getCommand());

		}
		commandLine = getCommandLineFromArgument(arg,
				workingDir,
				params);
		// final String[] parts = commandLine.split(" ");
		// final List<String> list = new
		// LinkedList<String>(Arrays.asList(commandLine));

		return this.launch(commandLine,
				workingDir,
				retries,
				redirectErrorStream,
				outputFile,
				errorFile);
	}

	private Process launchAsyncFromClosure(final Object arg)
			throws USMException {
		final Callable<?> closure = (Callable<?>) arg;
		Object retval;
		try {
			retval = closure.call();
		} catch (final Exception e) {
			logger.log(Level.SEVERE,
					"A closure entry failed to execute: " + e,
					e);
			throw new USMException("Launch of process from closure exited with an exception: " + e.getMessage(), e);
		}

		if (retval == null) {
			return null; // this is the expected behavior
		}

		if (retval instanceof Process) {
			return (Process) retval;
		}
		logger.warning("The Start closure returned a non-null value that is not a Process. "
				+ "This value will be ignored! Returned value was of type: "
				+ retval.getClass().getName() + ". Value was: " + retval);

		return null;
	}

	@Override
	public Object launchProcess(final ExecutableDSLEntry arg, final File workingDir, final int retries,
			final boolean redirectErrorStream, final Map<String, Object> params)
			throws USMException {

		final List<String> paramsList = getParamsListFromMap(params);
		if (arg.getEntryType() == ExecutableDSLEntryType.CLOSURE) {
			// in process execution of a closure
			final Closure<?> closure = ((ClosureExecutableEntry) arg).getCommand();

			try {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Closure Parameters: " + paramsList.toString());
				}

				return closure.call(paramsList.toArray());
			} catch (final Exception e) {
				logger.log(Level.SEVERE,
						"A closure entry failed to execute: " + e.getMessage(),
						e);
				throw new USMException("Failed to execute closure " + e.getMessage(), e);
			}
		}

		final Process proc = launchProcessAsync(arg,
				workingDir,
				retries,
				redirectErrorStream,
				paramsList);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

		String line = null;
		final StringBuilder sb = new StringBuilder();
		final String newline = System.getProperty("line.separator");

		logger.info("Command Output:");
		try {
			do {
				if (line != null) {
					sb.append(line).append(newline);
					logger.info(line);
				}
				line = reader.readLine();

			} while (line != null);
		} catch (final IOException ioe) {
			throw new USMException("Failed to execute command: " + commandLine, ioe);

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					// ignore
				}

			}

		}

		try {
			final int exitValue = proc.waitFor();
			logger.info("Command exited with value: " + exitValue);
			if (exitValue != 0) {
				logger.severe("Event lifecycle external process exited with abnormal status code: " + exitValue);

				final String result = sb.toString();
				final String exceptionReason = GroovyExceptionHandler.getExceptionString(result);

				logger.log(Level.SEVERE,
						"Event lifecycle external process failed: " + result);

				throw new USMException("Event lifecycle external process exited with abnormal status code: "
						+ exitValue + " " + exceptionReason);
			}
		} catch (final InterruptedException e) {
			logger.warning("Interrupted while waiting for process to exit");
		}

		// sleeping for a short interval, to make sure process table is cleaned
		// of the dead process
		try {
			Thread.sleep(POST_SYNC_PROCESS_SLEEP_INTERVAL);
		} catch (final InterruptedException e) {
			// ignore
		}

		return sb.toString();

	}

	private List<String> getParamsListFromMap(final Map<String, Object> params) {
		final List<String> paramsList = new ArrayList<String>();
		int index = 0;
		while (true) {
			final String param = (String) params.get(CloudifyConstants.INVOCATION_PARAMETERS_KEY + index);
			if (param != null) {
				paramsList.add(param);
			} else {
				break;
			}
			index++;
		}
		return paramsList;
	}

	@Override
	public String getCommandLine() {
		return this.commandLine.toString();
	}

	private void modifyCommandLine(final List<String> commandLineParams, final File workingDir, final File outputFile,
			final File errorFile)
			throws USMException {
		modifyOSCommandLine(commandLineParams, workingDir);

		if (commandLineParams.get(0).endsWith(".groovy")) {
			modifyGroovyCommandLine(commandLineParams,
					workingDir);
		}

		modifyRedirectionCommandLine(commandLineParams,
				outputFile,
				errorFile);

		if (ServiceUtils.isLinuxOrUnix()) {
			// run the whole command in a shell session
			logger.fine("Command before shell modification: " + commandLineParams);
			final StringBuilder sb = new StringBuilder();
			for (final String param : commandLineParams) {
				sb.append(param).append(" ");
			}
			commandLineParams.clear();
			commandLineParams.addAll(Arrays.asList("nohup",
					"sh",
					"-c",
					sb.toString()));
			logger.fine("Command after shell modification: " + commandLineParams);

		}
	}

	private void modifyRedirectionCommandLine(final List<String> commandLineParams, final File outputFile,
			final File errorFile) {
		if (outputFile == null) {
			return; // both files are set, or neither
		}

		commandLineParams.addAll(createRedirectionParametersForOS(outputFile,
				errorFile));

		// if(USMUtils.isLinuxOrUnix()) {
		// // add the terminating quotes
		// commandLineParams.add("\"");
		// }
	}

	private List<String> createRedirectionParametersForOS(final File outputFile, final File errorFile) {
		return Arrays.asList(">>", outputFile.getAbsolutePath(), "2>>", errorFile.getAbsolutePath());
	}

	private Process launch(final List<String> commandLineParams, final File workingDir, final int retries,
			final boolean redirectErrorStream, final File outputFile, final File errorFile)
			throws USMException {

		if (outputFile == null && errorFile != null || outputFile != null && errorFile == null) {
			throw new IllegalArgumentException("Both output and error files must be set, or none of them");
		}

		if (redirectErrorStream && (outputFile != null || errorFile != null)) {
			throw new IllegalArgumentException(
					"If redirectError option is chosen, neither output file or error file can be set");
		}

		modifyCommandLine(commandLineParams, workingDir, outputFile, errorFile);
		final String modifiedCommandLine =
				org.springframework.util.StringUtils.collectionToDelimitedString(commandLineParams,
						" ");

		this.commandLine = commandLineParams; // last command line to be
												// executed

		int attempt = 1;
		USMException ex = null;
		while (attempt <= retries + 1) {
			final ProcessBuilder pb = new ProcessBuilder(commandLineParams);
			pb.directory(workingDir);
			pb.redirectErrorStream(redirectErrorStream);
			final Map<String, String> env = createEnvironment();
			pb.environment().putAll(env);

			try {
				logger.fine("Parsed command line: " + commandLineParams.toString());

				final String fileInitialMessage =
						"Starting service process in working directory:'" + workingDir + "' "
								+ "at:'" + new Date() + "' with command:'" + commandLineParams + "'"
								+ System.getProperty("line.separator");
				if (outputFile != null) {
					appendMessageToFile(fileInitialMessage,
							outputFile);
				}
				if (errorFile != null) {
					appendMessageToFile(fileInitialMessage,
							errorFile);
				}
				return pb.start();
			} catch (final IOException e) {
				ex = new USMException("Failed to start process with command line: " + modifiedCommandLine, e);
				logger.log(Level.SEVERE,
						"Process start attempt number " + attempt + " failed",
						ex);
			}
			++attempt;
		}
		throw ex;
	}

	private void appendMessageToFile(final String msg, final File file)
			throws IOException {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			writer.write(msg);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private Map<String, String> createEnvironment() {
		final Map<String, String> map = new HashMap<String, String>();

		final String groupsProperty = getGroupsProperty();
		map.put("LOOKUPGROUPS",
				groupsProperty);

		final String locatorsProperty = getLocatorsProperty();
		if (locatorsProperty != null) {
			map.put("LOOKUPLOCATORS",
					locatorsProperty);
		}

		if (this.clusterInfo == null) {
			logger.warning("ClusterInfo in Process Launcher is null. "
					+ "Child process will have missing environment variables");
		} else {
			if (clusterInfo.getName() != null) {
				map.put(CloudifyConstants.USM_ENV_CLUSTER_NAME,
						clusterInfo.getName());
				final FullServiceName fullServiceName = ServiceUtils.getFullServiceName(clusterInfo.getName());
				map.put(CloudifyConstants.USM_ENV_APPLICATION_NAME,
						fullServiceName.getApplicationName());
				map.put(CloudifyConstants.USM_ENV_SERVICE_NAME,
						fullServiceName.getServiceName());

			} else {
				logger.warning("PU Name in ClusterInfo is null. "
						+ "This should never happen as of 2.5.0");

				map.put(CloudifyConstants.USM_ENV_CLUSTER_NAME,
						ServiceUtils.getAbsolutePUName(CloudifyConstants.DEFAULT_APPLICATION_NAME,
								"USM"));
				map.put(CloudifyConstants.USM_ENV_APPLICATION_NAME,
						CloudifyConstants.DEFAULT_APPLICATION_NAME);
				map.put(CloudifyConstants.USM_ENV_SERVICE_NAME,
						"USM");

			}
			map.put(CloudifyConstants.USM_ENV_PU_UNIQUE_NAME,
					clusterInfo.getUniqueName());
			map.put(CloudifyConstants.USM_ENV_INSTANCE_ID,
					"" + clusterInfo.getInstanceId());
			map.put(CloudifyConstants.USM_ENV_NUMBER_OF_INSTANCES,
					"" + clusterInfo.getNumberOfInstances());
			map.put(CloudifyConstants.USM_ENV_RUNNING_NUMBER,
					"" + clusterInfo.getRunningNumber());
			map.put(CloudifyConstants.USM_ENV_SERVICE_FILE_NAME,
					"" + this.configutaion.getServiceFile().getName());
		}

		if (groovyEnvironmentClassPath != null && !groovyEnvironmentClassPath.isEmpty()) {
			map.put("CLASSPATH",
					this.groovyEnvironmentClassPath);
		}

		// logger.info("Child process additional env variables: " + map);
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Adding Environment to child process: " + map);
		}
		return map;
	}

	private String getGroupsProperty() {
		String groupsProperty = System.getProperty("com.gs.jini_lus.groups");
		if (groupsProperty == null) {
			groupsProperty = System.getenv("LOOKUPGROUPS");
		}

		if (groupsProperty == null) {
			groupsProperty = "gigaspaces-" + PlatformVersion.getVersionNumber();
		}

		return groupsProperty.replace("\"", "");
	}

	private String getLocatorsProperty() {
		String property = System.getProperty("com.gs.jini_lus.locators");
		if (property == null) {
			property = System.getenv("LOOKUPLOCATORS");
		}
		if (property != null) {
			property = property.replace("\"",
					"");
		}
		return property;
	}

	@Override
	public Object launchProcess(final ExecutableDSLEntry arg, final File workingDir, final Map<String, Object> params)
			throws USMException {
		return launchProcess(arg, workingDir, 0, true, params);
	}

	@Override
	public Object launchProcess(final ExecutableDSLEntry arg, final File workingDir)
			throws USMException {
		return launchProcess(arg, workingDir, new HashMap<String, Object>());
	}

	@Override
	public Process launchProcessAsync(final ExecutableDSLEntry arg, final File workingDir, final File outputFile,
			final File errorFile)
			throws USMException {
		return launchAsync(arg, workingDir, 0, false, outputFile, errorFile, null);
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

	/**
	 * Converts the specified environment variables to system properties. Each environment variable is expected to have
	 * this value pattern: -DsystemPropertyName=systemPropertyValue or this: -DsystemPropertyName=systemPropertyValue
	 * -DAnotherSystemPropertyName=anotherSystemPropertyValue
	 *
	 * @param envVarsList
	 * @return
	 */
	private static List<String> convertEnvVarsToSysPropsList(final List<String> envVarsList) {
		final List<String> sysPropsList = new ArrayList<String>();
		String envVarValue;

		for (final String envVarName : envVarsList) {
			envVarValue = System.getenv(envVarName);
			if (envVarValue != null) {
				final String[] sysProps = envVarValue.split("-D");
				for (final String sysProp : sysProps) {
					if (StringUtils.isNotBlank(sysProp)) {
						sysPropsList.add("-D" + sysProp.trim());
					}
				}
			}
		}

		return sysPropsList;
	}

}
