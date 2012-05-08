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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.dsl.DSLConfiguration;
import org.hyperic.sigar.Sigar;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

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
	private UniversalServiceManagerConfiguration configutaion;

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

	private List<String> createLinuxCommandLineFromWindows(final List<String> windowsCommandLine, final File puWorkDir) {
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
		// final List<String> dirs = getJarDirsForGroovyClasspath(homeDir,
		// workingDir);
		if (jars != null) {
			// sb.append("\"");
			for (final File jar : jars) {
				sb.append(jar.getAbsolutePath()).append(File.pathSeparator);
			}
			// sb.append("\"");
		}

		// final List<String> dirs = getJarDirsForGroovyClasspath(homeDir,
		// workingDir);
		// if (dirs != null) {
		// sb.append("\"");
		// for (final String dir : dirs) {
		// sb.append(dir).append(File.pathSeparator);
		// }
		// sb.append("\"");
		// }

		final ArrayList<String> groovyCommandParams = new ArrayList<String>();
		groovyCommandParams.add(groovyPath);
		if (USMUtils.isWindows()) {
			modifyWindowsCommandLine(groovyCommandParams,
					workingDir);
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

		this.groovyCommandLinePrefixParams = groovyCommandParams;
	}

	private String createGroovyPath(final File homeDir)
			throws FileNotFoundException, USMException {
		final File toolsDir = new File(homeDir, "tools");
		final File groovyDir = new File(toolsDir, "groovy");
		final File binDir = new File(groovyDir, "bin");
		File groovyFile = null;
		if (USMUtils.isWindows()) {
			groovyFile = new File(binDir, "groovy.bat");
		} else {
			groovyFile = new File(binDir, "groovy");
		}

		if (!groovyFile.exists()) {
			throw new FileNotFoundException("Could not find groovy executable: " + groovyFile.getAbsolutePath());
		}

		if (USMUtils.isLinuxOrUnix()) {
			USMUtils.markFileAsExecutable(sigar,
					groovyFile);
		}

		return groovyFile.getAbsolutePath();

	}

	private void modifyOSCommandLine(final List<String> commandLineParams, final File puWorkDir)
			throws USMException {
		final String runParam = commandLineParams.get(0);

		if (USMUtils.isWindows()) {
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
		if (executeFile != null) {
			final File parent = executeFile.getParentFile();
			if (parent != null && parent.equals(puWorkDir)) {
				if (executeScriptName.endsWith(".sh") && !executeScriptName.startsWith(LINUX_EXECUTE_PREFIX)) {
					commandLineParams.remove(0);
					executeScriptName = LINUX_EXECUTE_PREFIX + executeScriptName;
					commandLineParams.add(0,
							executeScriptName);
				}
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
		for (final String part : parts) {
			list.add(part);

		}
		return list;
	}

	private List<String> getCommandLineFromArgument(final Object arg, final File workDir, final List<String> params) {
		if (arg instanceof String) {
			// Split the command into parts
			final List<String> commandLineStringInParts = convertCommandLineStringToParts((String) arg);
			// Add the custom command parameters as args to the split commands
			// array
			if (params != null) {
				commandLineStringInParts.addAll(params);
			}

			return commandLineStringInParts;
		}

		if (arg instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) arg;

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

		if (arg instanceof List<?>) {
			final List<?> originalList = (List<?>) arg;

			final List<String> resultList = new ArrayList<String>(originalList.size());
			for (final Object elem : originalList) {
				resultList.add(elem.toString());
			}
			return resultList;
		}
		throw new IllegalArgumentException("Could not find command line for argument " + arg + "!");

	}

	private List<String> getCommandLineFromValue(final Object value) {
		if (value instanceof String) {
			return convertCommandLineStringToParts((String) value);
		} else if (value instanceof List<?>) {
			@SuppressWarnings("unchecked")
			final List<String> result = (List<String>) value;
			return result;
		} else {
			throw new IllegalArgumentException("The value: " + value
					+ " could not be converted to a command line. Only a String, or a List of Strings may be used");
		}
	}

	private List<String> lookUpCommandLineForOS(final Map<String, Object> map, final String os) {
		final Set<Entry<String, Object>> entries = map.entrySet();
		for (final Entry<String, Object> entry : entries) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
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

	private List<String> createAlternativeCommandLine(final Map<String, Object> map, final File workDir) {
		if (USMUtils.isWindows()) {
			List<String> otherCommandLine = null;

			if (map.entrySet().size() > 1) {
				otherCommandLine = lookUpCommandLineForOS(map,
						"Linux");
			}

			if (otherCommandLine == null) {
				otherCommandLine = getCommandLineFromValue(map.values().iterator().next());
			}

			final List<String> alternativeCommandLine = createWindowsCommandLineFromLinux(otherCommandLine,
					workDir);
			return alternativeCommandLine;
		} else {
			List<String> otherCommandLine = null;

			if (map.entrySet().size() > 1) {
				otherCommandLine = lookUpCommandLineForOS(map,
						"Windows");
			}

			if (otherCommandLine == null) {
				otherCommandLine = getCommandLineFromValue(map.values().iterator().next());
			}

			final List<String> alternativeCommandLine = createLinuxCommandLineFromWindows(otherCommandLine,
					workDir);
			return alternativeCommandLine;

		}
	}

	@Override
	public Process launchProcessAsync(final Object arg, final File workingDir, final int retries,
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

	private Process launchAsync(final Object arg, final File workingDir, final int retries,
			final boolean redirectErrorStream, final File outputFile, final File errorFile, final List<String> params)
			throws USMException {
		if (arg instanceof Callable<?>) {
			// in process execution of a closure
			return launchAsyncFromClosure(arg);

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
		Object retval = null;
		try {
			retval = closure.call();
		} catch (final Exception e) {
			logger.log(Level.SEVERE,
					"A closure entry failed to execute: " + e,
					e);
			throw new USMException("Launch of process from closure exited with an exception: " + e.getMessage(), e);
		}

		if (retval == null) {
			throw new USMException("Launch of process from closure did not return a process handle!");
		}

		if (!(retval instanceof Process)) {
			throw new USMException(
					"Launch of process from closure returned a result that is not a process handle. Result was: "
							+ retval);
		}

		return (Process) retval;
	}

	@Override
	public Object launchProcess(final Object arg, final File workingDir, final int retries,
			final boolean redirectErrorStream, final Map<String, Object> params)
			throws USMException {

		final List<String> paramsList = getParamsListFromMap(params);
		if (arg instanceof Closure<?>) {
			// in process execution of a closure
			final Closure<?> closure = (Closure<?>) arg;

			try {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Closure Parameters: " + paramsList.toString());
				}
				// if (closure.getMaximumNumberOfParameters() != paramsList.size()){
				// USMException e = new USMException("Invalid number of parameters."
				// +" Expecting " + closure.getMaximumNumberOfParameters()
				// + " parameters, got " + paramsList.size()
				// + ": " + paramsList.toString());
				// logger.log(Level.SEVERE, e.getMessage());
				// throw e;
				// }
				// invoke the command closure.
				final Object result = closure.call(paramsList.toArray());
				return result;
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
				// TODO:Add result string to exception if not groovy exception.
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
		modifyOSCommandLine(commandLineParams,
				workingDir);

		if (commandLineParams.get(0).endsWith(".groovy")) {
			modifyGroovyCommandLine(commandLineParams,
					workingDir);
		}

		modifyRedirectionCommandLine(commandLineParams,
				outputFile,
				errorFile);

		if (USMUtils.isLinuxOrUnix()) {
			// run the whole command in a shell session
			logger.info("Command before shell modification: " + commandLineParams);
			final StringBuilder sb = new StringBuilder();
			for (final String param : commandLineParams) {
				sb.append(param).append(" ");
			}
			commandLineParams.clear();
			commandLineParams.addAll(Arrays.asList("nohup",
					"sh",
					"-c",
					sb.toString()));
			logger.info("Command after shell modification: " + commandLineParams);

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
		return Arrays.asList(">>",
				outputFile.getAbsolutePath(),
				"2>>",
				errorFile.getAbsolutePath());

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

		modifyCommandLine(commandLineParams,
				workingDir,
				outputFile,
				errorFile);
		final String modifiedCommandLine = StringUtils.collectionToDelimitedString(commandLineParams,
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

			Process proc;

			try {
				logger.info("Parsed command line: " + commandLineParams);
				final String fileInitialMessage =
						"Starting service process at: " + new Date() + " with command: " + commandLineParams
								+ System.getProperty("line.separator");
				if (outputFile != null) {
					appendMessageToFile(fileInitialMessage,
							outputFile);
				}
				if (errorFile != null) {
					appendMessageToFile(fileInitialMessage,
							errorFile);
				}
				proc = pb.start();
				return proc;
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
						+ "If running in the IntegratedProcessingUnitContainer, this is normal. "
						+ "Using 'USM' instead");
				// TODO - the process launcher does have access to the service object, so can't know the service name.
				// This could possibly be changed. This is useful for the test-recipe command, which will know the
				// real service name.
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
					"" + ((DSLConfiguration) this.configutaion).getServiceFile().getName());

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

		groupsProperty = groupsProperty.replace("\"",
				"");

		return groupsProperty;
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
	public Object launchProcess(final Object arg, final File workingDir, final Map<String, Object> params)
			throws USMException {
		return launchProcess(arg,
				workingDir,
				0,
				true,
				params);

	}

	@Override
	public Object launchProcess(final Object arg, final File workingDir)
			throws USMException {
		return launchProcess(arg,
				workingDir,
				new HashMap<String, Object>());

	}

	@Override
	public Process launchProcessAsync(final Object arg, final File workingDir, final File outputFile,
			final File errorFile)
			throws USMException {
		return launchAsync(arg,
				workingDir,
				0,
				false,
				outputFile,
				errorFile,
				null);
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

}
