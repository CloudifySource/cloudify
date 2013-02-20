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
package org.cloudifysource.shell.commands;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainer;

import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Tests a recipe.
 * 
 *        Required arguments: recipe - Path to recipe folder or packaged zip file
 * 
 *        Optional arguments: recipeTimeout - Number of seconds that the recipe should run, before shutdown is invoked
 *        (default: 30). serviceFileName - Name of the service file in the recipe folder, if not using the default.
 * 
 *        Command syntax: test-recipe [recipeTimeout] [serviceFileName] recipe
 */
@Command(scope = "cloudify", name = "test-recipe", description = "tests a recipe")
public class TestRecipe extends AbstractGSCommand {

	private static final int UNEXPECTED_ERROR_EXIT_CODE = -2;

	private static final int DEFAULT_RECIPE_TIMEOUT_SECS = 30;

	private static final String JAVA_HOME_ENV_VAR_NAME = "JAVA_HOME";

	private static final int EXTERNAL_PROCESS_WATCHDOG_ADDITIONAL_TIMEOUT = 120;

	@Argument(index = 0, required = true, name = "recipe", description = "Path to recipe folder or packaged zip file")
	private File recipeFolder;

	@Argument(index = 1, required = false, name = "recipeTimeout", description = "Number of seconds that the recipe"
			+ " should run, before shutdown is invoked. Defaults to 30.")
	private int timeout = DEFAULT_RECIPE_TIMEOUT_SECS;

	@Argument(index = 2, required = false, name = "serviceFileName", description = "Name of the service file in the "
			+ "recipe folder, if not using the default")
	private String serviceFileName;

	private static final String[] JAR_DIRS = { "lib/required", "tools/groovy/lib", "lib/platform/sigar",
			"lib/optional/spring", "lib/platform/usm", "lib/platform/cloudify" };

	/**
	 * Create a full classpath, including the existing classpath and additional paths to Jars and service files.
	 * 
	 * @param serviceFolder
	 *            The folder of the current service
	 * @return A full classpath
	 */
	private String createClasspathString(final File serviceFolder) {

		// Start with current environment variable
		String currentClassPathEnv = System.getenv("CLASSPATH");
		if (currentClassPathEnv == null) {
			currentClassPathEnv = "";
		}
		currentClassPathEnv += File.pathSeparator;
		final StringBuilder sb = new StringBuilder(currentClassPathEnv);

		// Add the required jar dirs
		for (final String jarDir : JAR_DIRS) {
			final File dir = getDirIfExists(jarDir);
			sb.append(
					dir.getAbsolutePath()).append(
					File.separator).append(
					"*").append(
					File.pathSeparator);
		}

		// finally, add the service folder to the recipe, so it finds the
		// META-INF files, and the lib dir
		sb.append(
				serviceFolder.getAbsolutePath()).append(
				File.separator).append(
				File.pathSeparator);
		sb.append(
				serviceFolder.getAbsolutePath()).append(
				File.separator).append("lib").append(File.separator).append("*")
				.append(File.pathSeparator);

		//sb.append(serviceFolder.getAbsolutePath() + "/ext/usmlib")
		// TODO - add local recipe jar files!
		return sb.toString();

	}

	/**
	 * Returns the directory (as a File object) if it exists. If the directory is not found in the given location or is
	 * not a directory - an IllegalStateException is thrown.
	 * 
	 * @param dirName
	 *            Directory name, relative to the home directory
	 * @return Directory as a File object.
	 */
	private File getDirIfExists(final String dirName) {
		final File requiredDir = new File(Environment.getHomeDirectory() + File.separator + dirName);
		if (!requiredDir.exists()) {
			throw new IllegalStateException("Could not find directory: " + dirName);
		}

		if (!requiredDir.isDirectory()) {
			throw new IllegalStateException(requiredDir + " is not a directory");
		}
		return requiredDir;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws CLIException {

		File serviceFolder = null;

		try {
			// First Package the recipe using the regular packager
			final File packagedRecipe = packageRecipe();

			// Then unzip the package in a temp location
			serviceFolder = createServiceFolder(packagedRecipe);
			logger.info("Executing service in temporary folder: " + serviceFolder);

			// verify that service configuration file contains a lifecycle
			// closure
			isServiceLifecycleNotNull(serviceFolder);

			// Create the classpath environment variable
			final String classpath = createClasspathString(serviceFolder);
			logger.fine("Setting Test Processing Unit's Classpath to: " + classpath);

			// and the command line
			final CommandLine cmdLine = createCommandLine();
			logger.fine("Setting Test Processing Unit's Command line to: " + cmdLine);

			// Create the environment for the command, using the current one
			// plus the new classpath
			final Map<Object, Object> env = new HashMap<Object, Object>();
			env.putAll(System.getenv());
			env.put("CLASSPATH", classpath);
			if (!env.containsKey(JAVA_HOME_ENV_VAR_NAME)) {
				final String javaHomeDirectory = getJavaHomeDirectory();
				logger.warning("JAVA_HOME system variables is not set. adding JAVA_HOME with value "
						+ javaHomeDirectory);
				env.put(JAVA_HOME_ENV_VAR_NAME, javaHomeDirectory);
				logger.info("JAVA_HOME was successfully set. added JAVA_HOME=" + javaHomeDirectory);
			}

			// Execute the command
			final int result = executeRecipe(
					cmdLine, env);
			if (result != 0) {
				if (result == 1) {
					logger.warning("Recipe exited abnormally with exit value 1. "
							+ "This may indicate that the external process did not shutdown on time and was"
							+ " forcibly shutdown by the execution watchdog.");
				}
				throw new CLIException(getFormattedMessage(
						"test_recipe_failure", result));
			}

		} finally {
			// Delete the temporary service folder
			if (serviceFolder != null) {
				try {
					FileUtils.deleteDirectory(serviceFolder);
				} catch (final IOException e) {
					logger.log(
							Level.SEVERE, "Failed to delete temporary service folder: " + serviceFolder, e);
				}
			}
		}

		return getFormattedMessage("test_recipe_success");

	}

	/**
	 * Gets the java home folder through the process ID of this process.
	 * 
	 * @return The path to the java folder
	 * @throws CLIException
	 *             Reporting a failure to retrieve the java home directory
	 */
	private String getJavaHomeDirectory()
			throws CLIException {
		try {
			final Sigar sigar = SigarHolder.getSigar();
			final long thisProcessPid = sigar.getPid();
			// get the java path of the current running process.
			final String javaFilePath = sigar.getProcExe(
					thisProcessPid).getName();
			final File javaFile = new File(javaFilePath);

			// Locate the java folder.
			final File javaFolder = javaFile.getParentFile().getParentFile();
			final String javaFolderPath = javaFolder.getAbsolutePath();
			return javaFolderPath;
		} catch (final SigarException e) {
			throw new CLIException("Failed to set the JAVA_HOME environment variable.", e);
		}
	}

	/**
	 * Verifies the service configuration is valid.
	 * 
	 * @param serviceFolder
	 *            The Folder holding the service configuration files
	 * @throws CLIException
	 *             Reporting a failure to find or parse the configuration files
	 */
	private void isServiceLifecycleNotNull(final File serviceFolder)
			throws CLIException {
		Service service;
		try {
			final File serviceFileDir = new File(serviceFolder, "ext");
			service = ServiceReader.getServiceFromDirectory(serviceFileDir).getService();

			if (service.getLifecycle() == null) {
				throw new CLIException(getFormattedMessage("test_recipe_service_lifecycle_missing"));
			}
		} catch (final DSLException e) {
			logger.log(
					Level.SEVERE, "DSL Parsing failed: " + e.getMessage(), e);
			e.printStackTrace();
			throw new CLIException("Packaging failed: " + e.getMessage(), e);

		}

	}

	/**
	 * This inner class reads and prints filtered text from a given source (BufferedReader). verbose mode - turns off
	 * the filtering of the data
	 */
	private static class FilteredOutputHandler implements Runnable {

		private final BufferedReader reader;
		private final boolean verbose;
		private static final String[] FILTERS = { "org.cloudifysource.dsl.internal.BaseServiceScript",
				"org.cloudifysource.usm.launcher.DefaultProcessLauncher", "org.cloudifysource.usm.USMEventLogger",
				"-Output]", "-Error]"

		};

		/**
		 * Constructor.
		 * 
		 * @param reader
		 *            This is the source text will be read from before filtering and possibly printing it.
		 * @param verbose
		 *            Sets filtering off when this value is true.
		 */
		FilteredOutputHandler(final BufferedReader reader, final boolean verbose) {
			this.reader = reader;
			this.verbose = verbose;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			while (true) {
				try {
					final String line = reader.readLine();
					if (line == null) {
						return;
					}
					if (this.verbose) {
						System.out.println(line);
					} else {
						for (final String filter : FILTERS) {

							if (line.contains(filter)) {
								System.out.println(line);
							}

						}
					}

				} catch (final IOException e) {
					return;
				}

			}
		}
	}

	/**
	 * Execute a command line in with a given map of environment settings. The execution outupt is filtered unless
	 * verbose is set to true.
	 * 
	 * @param cmdLine
	 *            The command to execute
	 * @param env
	 *            Environment settings available for the command execution
	 * @return the command's execution exit code, or -2 if the command failed to execute
	 */
	private int executeRecipe(final CommandLine cmdLine, final Map<Object, Object> env) {
		final DefaultExecutor executor = new DefaultExecutor();

		// The watchdog will terminate the process if it does not end within the
		// specified timeout
		final int externalProcessTimeout = (this.timeout + EXTERNAL_PROCESS_WATCHDOG_ADDITIONAL_TIMEOUT) * 1000;
		final ExecuteWatchdog watchdog = new TestRecipeWatchdog(externalProcessTimeout);
		executor.setWatchdog(watchdog);

		executor.setExitValue(0);
		int result = -1;

		PipedInputStream in = null;
		PipedOutputStream out = null;
		BufferedReader reader = null;
		try {
			in = new PipedInputStream();
			out = new PipedOutputStream(in);
			reader = new BufferedReader(new InputStreamReader(in));

			final Thread thread = new Thread(new FilteredOutputHandler(reader, this.verbose));
			thread.setDaemon(true);
			thread.start();

			final PumpStreamHandler psh = new PumpStreamHandler(out, out);
			executor.setStreamHandler(psh);
			result = executor.execute(
					cmdLine, env);
		} catch (final ExecuteException e) {
			logger.log(
					Level.SEVERE, "A problem was encountered while executing the recipe: " + e.getMessage(), e);
		} catch (final IOException e) {
			logger.log(
					Level.SEVERE, "An IO Exception was encountered while executing the recipe: " + e.getMessage(), e);
			result = UNEXPECTED_ERROR_EXIT_CODE;
		}

		return result;
	}

	/**
	 * Extracts the given file to a service folder.
	 * 
	 * @param packagedRecipe
	 *            The file to extract
	 * @return The service folder
	 */
	private File createServiceFolder(final File packagedRecipe) {
		File serviceFolder = null;
		try {
			serviceFolder = unzipFile(packagedRecipe);
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to unzip recipe: " + e.getMessage(), e);
		}
		return serviceFolder;
	}

	/**
	 * Packages the recipe folder. If the recipe folder is a file, verifies it is a zip or a jar.
	 * 
	 * @return Packaged recipe folder
	 * @throws CLIException
	 *             Reporting missing recipe folder, wrong file type or failure to pack the folder
	 */
	private File packageRecipe()
			throws CLIException {
		if (!recipeFolder.exists()) {
			throw new CLIStatusException(
					"service_file_doesnt_exist", recipeFolder.getAbsolutePath(), this.serviceFileName);
		}
		if (recipeFolder.isFile()) {
			if (recipeFolder.getName().endsWith(
					".zip") || recipeFolder.getName().endsWith(
					".jar")) {
				return recipeFolder;
			}
			throw new CLIStatusException("not_jar_or_zip", recipeFolder.getAbsolutePath(), this.serviceFileName);
		}

		// it's a folder
		File dslDirOrFile = recipeFolder;

		if (serviceFileName != null) {
			// use non default service file
			dslDirOrFile = new File(dslDirOrFile, serviceFileName);
		}
		return doPack(dslDirOrFile);
	}

	/**
	 * Packages the recipe files and other required files in a zip.
	 * 
	 * @param recipeDirOrFile
	 *            The recipe service DSL file or containing folder
	 * @return A zip file
	 * @throws CLIException
	 *             Reporting a failure to find or parse the given DSL file, or pack the zip file
	 */
	public File doPack(final File recipeDirOrFile)
			throws CLIException {
		try {
			return Packager.pack(recipeDirOrFile);
		} catch (final IOException e) {
			throw new CLIException(e);
		} catch (final PackagingException e) {
			throw new CLIException(e);
		} catch (final DSLException e) {
			throw new CLIException(e);
		}
	}

	/**
	 * Create a complete command line, including path and arguments.
	 * 
	 * @return Configured command line, ready for execution
	 */
	private CommandLine createCommandLine() {
		final String javaPath = getJavaPath();

		final String gsHome = Environment.getHomeDirectory();
		final String[] commandParams =
				{ "-Dcom.gs.home=" + gsHome, "-Dorg.hyperic.sigar.path=" + gsHome + "/lib/platform/sigar",
						"-Dcom.gs.usm.RecipeShutdownTimeout=" + timeout,
						IntegratedProcessingUnitContainer.class.getName(), "-cluster", "id=1", "total_members=1" };
		final CommandLine cmdLine = new CommandLine(javaPath);

		for (final String param : commandParams) {
			cmdLine.addArgument(param);
		}
		if (this.serviceFileName != null) {
			cmdLine.addArgument("-properties");
			cmdLine.addArgument("embed://" + CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME + "="
					+ this.serviceFileName);
		}

		// -Dcom.gs.usm.RecipeShutdownTimeout=10

		return cmdLine;
	}

	/**
	 * Gets java path via sigar from the current process.
	 * 
	 * @return java path
	 */
	private String getJavaPath() {
		final long pid = SigarHolder.getSigar().getPid();
		try {
			return SigarHolder.getSigar().getProcExe(
					pid).getName();
		} catch (final SigarException e) {
			throw new IllegalStateException("Failed to read java path via sigar from current process (" + pid + ")", e);
		}
	}

	/**
	 * Creates a temporary folder.
	 * 
	 * @return Temporary folder
	 * @throws IOException
	 *             Reporting a failure to create the folder
	 */
	protected static File createTempDir()
			throws IOException {
		File targetDir = null;
		final String tmpDir = System.getProperty("java.io.tmpdir");
		if (tmpDir == null) {
			throw new IllegalStateException(
					"The java.io.tmpdir property is null. Can't create a temporary directory for service unpacking");
		}
		if (tmpDir.indexOf(' ') >= 0) {

			targetDir = new File("Recipe_Test_Temp_Files" + File.separator + "Test_" + System.currentTimeMillis());
			if (!targetDir.mkdirs()) {
				throw new IllegalStateException(
						"Failed to create a directory where service will be unzipped. Target was: " + targetDir);
			}
			logger.warning("The System temp directory name includes spaces. Using alternate directory: " + targetDir);

		}
		final File tempFile = File.createTempFile(
				"GS_tmp_dir", ".service", targetDir);
		final String path = tempFile.getAbsolutePath();
		tempFile.delete();
		tempFile.mkdirs();
		final File baseDir = new File(path);
		return baseDir;
	}

	/**
	 * Unzips a given file.
	 * 
	 * @param inputFile
	 *            The zip file to extract
	 * @return The new folder, containing the extracted content of the zip file
	 * @throws IOException
	 *             Reporting a failure to extract the zipped file or close it afterwards
	 */
	private static File unzipFile(final File inputFile)
			throws IOException {

		ZipFile zipFile = null;
		try {
			final File baseDir = TestRecipe.createTempDir();
			zipFile = new ZipFile(inputFile);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();

				if (entry.isDirectory()) {

					logger.fine("Extracting directory: " + entry.getName());
					final File dir = new File(baseDir, entry.getName());
					dir.mkdir();
					continue;
				}

				logger.finer("Extracting file: " + entry.getName());
				final File file = new File(baseDir, entry.getName());
				file.getParentFile().mkdirs();
				ServiceReader.copyInputStream(
						zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(file)));
			}
			return baseDir;

		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (final IOException e) {
					logger.log(
							Level.SEVERE, "Failed to close zip file after unzipping zip contents", e);
				}
			}
		}

	}

	/***********
	 * Workaround accessor to prevent eclipse clean up from turning the timeout field to a final field.
	 * 
	 * @param timeout
	 *            .
	 */
	void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

}
