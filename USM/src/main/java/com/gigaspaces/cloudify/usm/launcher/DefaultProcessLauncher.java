package com.gigaspaces.cloudify.usm.launcher;

import groovy.lang.Closure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.hyperic.sigar.Sigar;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.usm.USMException;
import com.gigaspaces.cloudify.usm.USMUtils;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.dsl.DSLConfiguration;
import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;
import com.j_spaces.kernel.PlatformVersion;

public class DefaultProcessLauncher implements ProcessLauncher,
		ClusterInfoAware {

	private static final int POST_SYNC_PROCESS_SLEEP_INTERVAL = 200;
	private static final String LINUX_EXECUTE_PREFIX = "./";
	private static final String[] WINDOWS_BATCH_FILE_PREFIX_PARAMS = {
			"cmd.exe", "/c " };
	private List<String> groovyCommandLinePrefixParams;
	// last command line to be executed
	private List<String> commandLine;
	private final Sigar sigar = SigarHolder.getSigar();
	private ClusterInfo clusterInfo;
	private String groovyEnvironmentClassPath;

	@Autowired
	private UniversalServiceManagerConfiguration configutaion;

	private List<String> switchFirstPartOfCommandLine(
			final List<String> originalCommandLine, final String newPart) {

		List<String> newCommand = new LinkedList<String>();
		newCommand.add(newPart);
		newCommand.addAll(originalCommandLine.subList(1,
				originalCommandLine.size()));
		return newCommand;

	}

	private List<String> createWindowsCommandLineFromLinux(
			final List<String> linuxCommandLine, final File puWorkDir) {

		final AlternativeExecutableFileNameFilter filter = new AlternativeExecutableFileNameFilter() {
			String prefix;

			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(prefix)
						&& (name.endsWith(".bat") || name.endsWith(".exe"));

			}

			@Override
			public void setPrefix(final String prefix) {
				this.prefix = prefix;

			}
		};

		return createCommandLineFromAlternativeOS(puWorkDir, linuxCommandLine,
				filter);

	}

	interface AlternativeExecutableFileNameFilter extends FilenameFilter {
		void setPrefix(String prefix);
	}

	private List<String> createCommandLineFromAlternativeOS(
			final File puWorkDir, final List<String> alternateCommandLine,
			final AlternativeExecutableFileNameFilter fileNameFilter) {

		if ((alternateCommandLine == null)
				|| (alternateCommandLine.size() == 0)) {
			return null;
		}

		final String executable = alternateCommandLine.get(0);// getFirstPartOfCommandLine(alternateCommandLine);

		// groovy files are executable on all OS
		if (executable.endsWith(".groovy")) {
			return alternateCommandLine;
		}

		final File executableFile = getFileFromRelativeOrAbsolutePath(
				puWorkDir, executable);
		if (executableFile == null) {
			logger.warning("Could not find an executable file: " + executable
					+ "in working directory " + puWorkDir
					+ " in command line: " + alternateCommandLine
					+ ". Alternate command line can't be created.");
			return null;
		}

		final String prefix = getFilePrefix(executableFile);

		final File parentDir = executableFile.getParentFile();

		fileNameFilter.setPrefix(prefix);
		final File[] files = parentDir.listFiles(fileNameFilter);

		if (files.length == 0) {
			return null;
		}

		logger.info("Found candidates for command line to replace "
				+ executable + ". Candidates are: " + Arrays.toString(files));

		final List<String> modifiedCommandLine = switchFirstPartOfCommandLine(
				alternateCommandLine, files[0].getName());

		return modifiedCommandLine;

	}

	protected String getFilePrefix(final File executableFile) {
		String prefixToSearch = executableFile.getName();
		final int index = prefixToSearch.lastIndexOf('.');
		if (index >= 0) {
			prefixToSearch = prefixToSearch.substring(0, index);
		}
		return prefixToSearch;
	}

	private List<String> createLinuxCommandLineFromWindows(
			final List<String> windowsCommandLine, final File puWorkDir) {
		final AlternativeExecutableFileNameFilter filter = new AlternativeExecutableFileNameFilter() {
			String prefix;

			@Override
			public boolean accept(final File dir, final String name) {
				return name.equals(prefix)
						|| (name.startsWith(prefix) && name.endsWith(".sh"));
			}

			@Override
			public void setPrefix(final String prefix) {
				this.prefix = prefix;

			}
		};

		return createCommandLineFromAlternativeOS(puWorkDir,
				windowsCommandLine, filter);

	}

	private void modifyGroovyCommandLine(final List<String> commandLineParams,
			final File workingDir) throws USMException {

		try {
			initGroovyCommandLine(workingDir);
		} catch (final FileNotFoundException e) {
			throw new USMException("Failed to set up groovy command line", e);
		}

		for (int i = 0; i < groovyCommandLinePrefixParams.size(); i++) {
			commandLineParams.add(i, groovyCommandLinePrefixParams.get(i));
		}
	}

	private void addJarsFromDirectoryToList(final File dir,
			final List<File> list) {
		final File[] jars = getJarFilesFromDir(dir);
		if (jars != null) { // possibe if directory does not exist
			list.addAll(Arrays.asList(jars));
		}
	}

	private List<File> getJarFilesForGroovyClasspath(final File gsHome,
			final File workingDir) {
		final List<File> list = new LinkedList<File>();

		// jar files in PU's lib dir
		addJarsFromDirectoryToList(new File(workingDir.getParentFile(), "lib"),
				list);

		// <GS_HOME>/lib/required
		addJarsFromDirectoryToList(new File(gsHome, "lib/required"), list);

		// <GS_HOME>/lib/platform/usm
		addJarsFromDirectoryToList(new File(gsHome, "lib/platform/usm"), list);
		addJarsFromDirectoryToList(new File(gsHome, "lib/platform/sigar"), list);

		return list;

	}

	// private List<String> getJarDirsForGroovyClasspath(final File gsHome,
	// final File workingDir) {
	// final List<String> list = new LinkedList<String>();
	//
	// final String wildcardSuffix = File.separator + "*";
	// // jar files in PU's lib dir
	// list.add(new File(workingDir.getParentFile(), "lib").getAbsolutePath()
	// + wildcardSuffix);
	//
	// // <GS_HOME>/lib/required
	// list.add(new File(gsHome, "lib/required").getAbsolutePath()
	// + wildcardSuffix);
	//
	// // <GS_HOME>/lib/platform/usm
	// list.add(new File(gsHome, "lib/platform/usm").getAbsolutePath()
	// + wildcardSuffix);
	// list.add(new File(gsHome, "lib/platform/sigar").getAbsolutePath()
	// + wildcardSuffix);
	//
	// return list;
	//
	// }

	protected File[] getJarFilesFromDir(final File dir) {
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
		logger.info("Setting ClassPath environment variable for child processes to: "
				+ classPathEnv);
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
			throw new FileNotFoundException(
					"Could not find groovy executable: "
							+ groovyFile.getAbsolutePath());
		}

		if (USMUtils.isLinuxOrUnix()) {
			USMUtils.markFileAsExecutable(sigar, groovyFile);
		}

		return groovyFile.getAbsolutePath();

	}

	private void modifyOSCommandLine(final List<String> commandLineParams,
			final File puWorkDir) throws USMException {
		final String runParam = commandLineParams.get(0);

		if (USMUtils.isWindows()) {
			modifyWindowsCommandLine(commandLineParams, puWorkDir);
		} else {
			markLinuxTargetAsExecutable(runParam, puWorkDir);
			modifyLinuxCommandLine(commandLineParams, puWorkDir);
		}

		/*
		 * else { throw new
		 * IllegalArgumentException("Unsupported Operating system: " +
		 * System.getProperty("os.name")); }
		 */

	}

	// Add "./" to command line, if not present and file is in ext dir
	private void modifyLinuxCommandLine(final List<String> commandLineParams,
			final File puWorkDir) {
		String executeScriptName = commandLineParams.get(0);
		final File executeFile = new File(puWorkDir, executeScriptName);
		if (executeFile != null) {
			final File parent = executeFile.getParentFile();
			if ((parent != null) && parent.equals(puWorkDir)) {
				if (executeScriptName.endsWith(".sh")
						&& !executeScriptName.startsWith(LINUX_EXECUTE_PREFIX)) {
					commandLineParams.remove(0);
					executeScriptName = LINUX_EXECUTE_PREFIX
							+ executeScriptName;
					commandLineParams.add(0, executeScriptName);
				}
			}
		}
	}

	private void modifyWindowsCommandLine(final List<String> commandLineParams,
			final File workingDir) {
		final String firstParam = commandLineParams.get(0);
		if (firstParam.endsWith(".bat")) {
			for (int i = 0; i < WINDOWS_BATCH_FILE_PREFIX_PARAMS.length; i++) {
				commandLineParams.add(i, WINDOWS_BATCH_FILE_PREFIX_PARAMS[i]);
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
				commandLineParams.add(i, WINDOWS_BATCH_FILE_PREFIX_PARAMS[i]);
			}
		}

	}

	private File getFileFromRelativeOrAbsolutePath(final File puWorkDir,
			final String fileName) {
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

	protected void markLinuxTargetAsExecutable(
			final String originalCommandLineRunParam, final File puWorkDir)
			throws USMException {

		logger.info("Setting File as executable: "
				+ originalCommandLineRunParam);
		final File file = getFileFromRelativeOrAbsolutePath(puWorkDir,
				originalCommandLineRunParam);
		if (file != null) {
			logger.info("File: " + file + " will be marked as executable");
			USMUtils.markFileAsExecutable(sigar, file);
		}

	}

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DefaultProcessLauncher.class.getName());

	private List<String> convertCommandLineStringToParts(
			final String commandLine) {
		List<String> list = new LinkedList<String>();
		String[] parts = commandLine.split(" ");
		for (String part : parts) {
			list.add(part);

		}
		return list;

	}

	private List<String> getCommandLineFromArgument(final Object arg,
			final File workDir) {
		if (arg instanceof String) {
			return convertCommandLineStringToParts((String) arg);
		}

		if (arg instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) arg;

			final String os = System.getProperty("os.name");
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Looking for command line for Operating System Name: "
						+ os);
			}

			final List<String> cmdLineList = lookUpCommandLineForOS(map, os);
			if (cmdLineList != null) {
				return cmdLineList;// convertCommandLineStringToParts(cmdLineList);
			}

			logger.severe("Could not find a matching operating system expression for Operating System: "
					+ os);
			logger.severe("Attempting alternative command line: " + os);

			final List<String> alternativeCommandLine = createAlternativeCommandLine(
					map, workDir);
			if (alternativeCommandLine != null) {
				return alternativeCommandLine;
			}
			logger.severe("Could not create alternative command line: " + os);

		}

		if (arg instanceof List<?>) {
			List<?> originalList = (List<?>) arg;

			List<String> resultList = new ArrayList<String>(originalList.size());
			for (Object elem : originalList) {
				resultList.add(elem.toString());
			}
			return resultList;
		}
		throw new IllegalArgumentException(
				"Could not find command line for argument " + arg + "!");

	}

	private List<String> getCommandLineFromValue(final Object value) {
		if (value instanceof String) {
			return convertCommandLineStringToParts((String) value);
		} else if (value instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) value;
			return result;
		} else {
			throw new IllegalArgumentException(
					"The value: "
							+ value
							+ " could not be converted to a command line. Only a String, or a List of Strings may be used");
		}
	}

	private List<String> lookUpCommandLineForOS(final Map<String, Object> map,
			final String os) {
		final Set<Entry<String, Object>> entries = map.entrySet();
		for (final Entry<String, Object> entry : entries) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			try {
				if (Pattern.matches(key, os)) {
					return getCommandLineFromValue(value);

				}
			} catch (final PatternSyntaxException pse) {
				logger.log(
						Level.WARNING,
						"Opeating System regular expression pattern: "
								+ key
								+ " cannot be compiled. It will me compared using basic string matching.",
						pse);
				if (key.equals(os)) {
					return getCommandLineFromValue(value);
				}
			}

		}
		return null;
	}

	private List<String> createAlternativeCommandLine(
			final Map<String, Object> map, final File workDir) {
		if (USMUtils.isWindows()) {
			List<String> otherCommandLine = null;

			if (map.entrySet().size() > 1) {
				otherCommandLine = lookUpCommandLineForOS(map, "Linux");
			}

			if (otherCommandLine == null) {
				otherCommandLine = getCommandLineFromValue(map.values()
						.iterator().next());
			}

			final List<String> alternativeCommandLine = createWindowsCommandLineFromLinux(
					otherCommandLine, workDir);
			return alternativeCommandLine;
		} else {
			List<String> otherCommandLine = null;

			if (map.entrySet().size() > 1) {
				otherCommandLine = lookUpCommandLineForOS(map, "Windows");
			}

			if (otherCommandLine == null) {
				otherCommandLine = getCommandLineFromValue(map.values()
						.iterator().next());
			}

			final List<String> alternativeCommandLine = createLinuxCommandLineFromWindows(
					otherCommandLine, workDir);
			return alternativeCommandLine;

		}
	}

	@Override
	public Process launchProcessAsync(final Object arg, final File workingDir,
			final int retries, final boolean redirectErrorStream)
			throws USMException {
		return launchAsync(arg, workingDir, retries, redirectErrorStream, null,
				null);
	}

	private Process launchAsync(final Object arg, final File workingDir,
			final int retries, final boolean redirectErrorStream,
			final File outputFile, final File errorFile) throws USMException {
		if (arg instanceof Callable<?>) {
			// in process execution of a closure
			return launchAsyncFromClosure(arg);

		}
		commandLine = getCommandLineFromArgument(arg, workingDir);
		// final String[] parts = commandLine.split(" ");
		// final List<String> list = new
		// LinkedList<String>(Arrays.asList(commandLine));

		return this.launch(commandLine, workingDir, retries,
				redirectErrorStream, outputFile, errorFile);
	}

	protected Process launchAsyncFromClosure(final Object arg)
			throws USMException {
		final Callable<?> closure = (Callable<?>) arg;
		Object retval = null;
		try {
			retval = closure.call();
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "A closure entry failed to execute: " + e,
					e);
			throw new USMException(
					"Launch of process from closure exited with an exception: "
							+ e.getMessage(), e);
		}

		if (retval == null) {
			throw new USMException(
					"Launch of process from closure did not return a process handle!");
		}

		if (!(retval instanceof Process)) {
			throw new USMException(
					"Launch of process from closure returned a result that is not a process handle. Result was: "
							+ retval);
		}

		return (Process) retval;
	}

	@Override
	public Object launchProcess(final Object arg, final File workingDir,
			final int retries, final boolean redirectErrorStream,
			Map<String, Object> params) throws USMException {

		if (arg instanceof Closure<?>) {
			// in process execution of a closure
			final Closure<?> closure = (Closure<?>) arg;

			try {
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					if (entry.getKey() != CloudifyConstants.INVOCATION_PARAMETER_COMMAND_NAME) {
						logger.fine("Adding parameter " + entry.getKey()
								+ " having value of " + entry.getValue());
						closure.setProperty(entry.getKey(), entry.getValue());
					}
				}
				// closure.setProperty(property, newValue)

				// closure.setResolveStrategy(Closure.TO_SELF);
				Object result = closure.call();
				return result;
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "A closure entry failed to execute: "
						+ e.getMessage(), e);
				throw new USMException("Failed to execute closure "
						+ e.getMessage(), e);
			}
		}

		final Process proc = launchProcessAsync(arg, workingDir, retries,
				redirectErrorStream);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));

		String line = null;
		StringBuilder sb = new StringBuilder();
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
			throw new USMException("Failed to execute command: " + commandLine,
					ioe);

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
				logger.severe("Event lifecycle external process exited with abnormal status code: "
						+ exitValue);
				throw new USMException(
						"Event lifecycle external process exited with abnormal status code: "
								+ exitValue);
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

	@Override
	public String getCommandLine() {
		return this.commandLine.toString();
	}

	private void modifyCommandLine(final List<String> commandLineParams,
			final File workingDir, File outputFile, File errorFile)
			throws USMException {
		modifyOSCommandLine(commandLineParams, workingDir);

		if (commandLineParams.get(0).endsWith(".groovy")) {
			modifyGroovyCommandLine(commandLineParams, workingDir);
		}

		modifyRedirectionCommandLine(commandLineParams, outputFile, errorFile);
	}

	private void modifyRedirectionCommandLine(
			final List<String> commandLineParams, final File outputFile,
			final File errorFile) {
		if (outputFile == null) {
			return; // both files are set, or neither
		}

		commandLineParams.addAll(createRedirectionParametersForOS(outputFile,
				errorFile));
	}

	private List<String> createRedirectionParametersForOS(
			final File outputFile, final File errorFile) {
			return Arrays.asList(">>", outputFile.getAbsolutePath(), "2>>",
					errorFile.getAbsolutePath());

	}

	private Process launch(final List<String> commandLineParams,
			final File workingDir, final int retries,
			final boolean redirectErrorStream, File outputFile, File errorFile)
			throws USMException {

		if (((outputFile == null) && (errorFile != null))
				|| ((outputFile != null) && (errorFile == null))) {
			throw new IllegalArgumentException(
					"Both output and error files must be set, or none of them");
		}

		if (redirectErrorStream
				&& ((outputFile != null) || (errorFile != null))) {
			throw new IllegalArgumentException(
					"If redirectError option is chosen, neither output file or error file can be set");
		}

		modifyCommandLine(commandLineParams, workingDir, outputFile, errorFile);
		final String modifiedCommandLine = StringUtils
				.collectionToDelimitedString(commandLineParams, " ");

		this.commandLine = commandLineParams; // last command line to be
												// executed

		int attempt = 1;
		USMException ex = null;
		while (attempt <= (retries + 1)) {
			final ProcessBuilder pb = new ProcessBuilder(commandLineParams);
			pb.directory(workingDir);
			pb.redirectErrorStream(redirectErrorStream);
			final Map<String, String> env = createEnvironment();
			pb.environment().putAll(env);

			Process proc;

			try {

				logger.info("Executing command line: " + modifiedCommandLine);
				proc = pb.start();
				return proc;
			} catch (final IOException e) {
				ex = new USMException(
						"Failed to start process with command line: "
								+ modifiedCommandLine, e);
				logger.log(Level.SEVERE, "Process start attempt number "
						+ attempt + " failed", ex);
			}
			++attempt;
		}
		throw ex;
	}

	private Map<String, String> createEnvironment() {
		final Map<String, String> map = new HashMap<String, String>();

		final String groupsProperty = getGroupsProperty();
		map.put("LOOKUPGROUPS", groupsProperty);

		final String locatorsProperty = getLocatorsProperty();
		if (locatorsProperty != null) {
			map.put("LOOKUPLOCATORS", locatorsProperty);
		}

		if (this.clusterInfo == null) {
			logger.warning("ClusterInfo in Process Launcher is null. Child process will have missing environment variables");
		} else {
			if (clusterInfo.getName() != null) {
				map.put(CloudifyConstants.USM_ENV_CLUSTER_NAME,
						clusterInfo.getName());
			} else {
				logger.warning("PU Name in ClusterInfo is null. "
						+ "If running in the IntegratedProcessingUnitContainer, this is normal. "
						+ "Using 'USM' instead");
				map.put(CloudifyConstants.USM_ENV_CLUSTER_NAME, "USM");
			}
			map.put(CloudifyConstants.USM_ENV_PU_UNIQUE_NAME,
					clusterInfo.getUniqueName());
			map.put(CloudifyConstants.USM_ENV_INSTANCE_ID,
					"" + clusterInfo.getInstanceId());
			map.put(CloudifyConstants.USM_ENV_NUMBER_OF_INSTANCES, ""
					+ clusterInfo.getNumberOfInstances());
			map.put(CloudifyConstants.USM_ENV_RUNNING_NUMBER,
					"" + clusterInfo.getRunningNumber());
			map.put(CloudifyConstants.USM_ENV_SERVICE_FILE_NAME, ""
					+ ((DSLConfiguration) this.configutaion).getServiceFile()
							.getName());

		}

		if (this.groovyEnvironmentClassPath != null
				&& this.groovyEnvironmentClassPath.length() > 0) {
			map.put("CLASSPATH", this.groovyEnvironmentClassPath);
		}

		// logger.info("Child process additional env variables: " + map);
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Adding Environment to child process: " + map);
		}

		return map;

	}

	protected String getGroupsProperty() {
		String groupsProperty = System.getProperty("com.gs.jini_lus.groups");
		if (groupsProperty == null) {
			groupsProperty = System.getenv("LOOKUPGROUPS");
		}

		if (groupsProperty == null) {
			groupsProperty = "gigaspaces-" + PlatformVersion.getVersionNumber();
		}

		groupsProperty = groupsProperty.replace("\"", "");

		return groupsProperty;
	}

	protected String getLocatorsProperty() {
		String property = System.getProperty("com.gs.jini_lus.locators");
		if (property == null) {
			property = System.getenv("LOOKUPLOCATORS");
		}
		if (property != null) {
			property = property.replace("\"", "");
		}
		return property;
	}

	@Override
	public Object launchProcess(final Object arg, final File workingDir,
			Map<String, Object> params) throws USMException {
		return launchProcess(arg, workingDir, 0, true, params);

	}

	@Override
	public Object launchProcess(final Object arg, final File workingDir)
			throws USMException {
		return launchProcess(arg, workingDir, new HashMap<String, Object>());

	}

	@Override
	public Process launchProcessAsync(final Object arg, final File workingDir,
			final File outputFile, final File errorFile) throws USMException {
		return launchAsync(arg, workingDir, 0, false, outputFile, errorFile);
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

}
