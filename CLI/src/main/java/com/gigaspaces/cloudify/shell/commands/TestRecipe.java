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
package com.gigaspaces.cloudify.shell.commands;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import org.hyperic.sigar.SigarException;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainer;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.shell.rest.ErrorStatusException;
import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(scope = "cloudify", name = "test-recipe", description = "tests a recipe")
public class TestRecipe extends AbstractGSCommand {

	private static final int EXTERNAL_PROCESS_WATCHDOG_TIMEOUT = 15;

	@Argument(index = 0, required = true, name = "recipe", description = "Path to recipe folder or packaged zip file")
	private File recipeFolder;

	@Argument(index = 1, required = false, name = "recipeTimeout", description = "Number of seconds that the recipe should run, before shutdown is invoked. Defaults to 30.")
	private int timeout = 30;

	@Argument(index = 2, required = false, name = "serviceFileName", description = "Name of the service file in the recipe folder, if not using the default")
	private String serviceFileName;

	private static final String[] JAR_DIRS = { "lib/required",
			"tools/groovy/lib", "lib/platform/sigar", "lib/optional/spring",
			"lib/platform/usm", };

	private String createClasspathString(File serviceFolder) {

		// Start with current environment variable
		String currentClassPathEnv = System.getenv("CLASSPATH");
		if (currentClassPathEnv == null) {
			currentClassPathEnv = "";
		}
		currentClassPathEnv += File.pathSeparator;
		StringBuilder sb = new StringBuilder(currentClassPathEnv);

		// Add the reqruired jar dirs
		for (String jarDir : JAR_DIRS) {
			File dir = getDirIfExists(jarDir);
			sb.append(dir.getAbsolutePath()).append(File.separator).append("*")
					.append(File.pathSeparator);
		}

		// finally, add the service folder to the recipe, so it finds the
		// META-INF files, and the lib dir
		sb.append(serviceFolder.getAbsolutePath()).append(File.separator)
				.append(File.pathSeparator);

		// TODO - add local recipe jar files!
		return sb.toString();

	}

	private File getDirIfExists(String dirName) {
		File requiredDir = new File(Environment.getHomeDirectory()
				+ File.separator + dirName);
		if (!requiredDir.exists()) {
			throw new IllegalStateException("Could not find directory: "
					+ dirName);
		}

		if (!requiredDir.isDirectory()) {
			throw new IllegalStateException(requiredDir + " is not a directory");
		}
		return requiredDir;
	}

	@Override
	protected Object doExecute() throws CLIException {

		File serviceFolder = null;

		try {
			// First Package the recipe using the regular packager
			File packagedRecipe = packageRecipe();


			// Then unzip the package in a temp location
			serviceFolder = createServiceFolder(packagedRecipe);
			logger.info("Executing service in temporary folder: "
					+ serviceFolder);
			
			//verify that service configuration file contains a lifecycle closure 
			isServiceLifecycleNotNull(serviceFolder);
			
			// Create the classpath environment variable
			final String classpath = createClasspathString(serviceFolder);
			logger.fine("Setting Test Processing Unit's Classpath to: "
					+ classpath);

			// and the command line
			CommandLine cmdLine = createCommandLine();
			logger.fine("Setting Test Processing Unit's Command line to: "
					+ cmdLine);

			// Create the environment for the command, using the current one
			// plus the new classpath
			Map<Object, Object> env = new HashMap<Object, Object>();
			env.putAll(System.getenv());
			env.put("CLASSPATH", classpath);

			// Execute the command
			int result = executeRecipe(cmdLine, env);
			if (result != 0) {
				if (result == 1) {
					logger.warning("Recipe exited abnormally with exit value 1. "
							+ "This may indicate that the external process did not shutdown on time and was forcibly shutdown by the execution watchdog.");
				}
				throw new CLIException(getFormattedMessage(
						"test_recipe_failure", result));
			}

		} finally {
			// Delete the temporary service folder
			if (serviceFolder != null) {
				try {
					FileUtils.deleteDirectory(serviceFolder);
				} catch (IOException e) {
					logger.log(Level.SEVERE,
							"Failed to delete temporary service folder: "
									+ serviceFolder, e);
				}
			}
		}

		return getFormattedMessage("test_recipe_success");

	}

	private void isServiceLifecycleNotNull(File serviceFolder) throws CLIException {
		Service service;
		try {
			File serviceFileDir = new File(serviceFolder, "ext");
			service = ServiceReader.getServiceFromDirectory(serviceFileDir, CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
			if (service.getLifecycle() == null){
				throw new CLIException(getFormattedMessage(
				"test_recipe_service_lifecycle_missing"));
			}
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Service configuration file not found " + e.getMessage(), e);
			throw new CLIException(
					"Failed to locate service configuration file. " + e.getMessage(), e);
		} catch (PackagingException e) {
			logger.log(Level.SEVERE, "Packaging failed: " + e.getMessage(), e);
			e.printStackTrace();
			throw new CLIException(
					"Packaging failed: " + e.getMessage(), e);
		}
		
	}

	private static class FilteredOutputHandler implements Runnable {
		private BufferedReader reader;
		private boolean verbose;
		private static final String[] FILTERS = {
				"com.gigaspaces.cloudify.dsl.internal.BaseServiceScript",
				"com.gigaspaces.cloudify.usm.launcher.DefaultProcessLauncher",
				"com.gigaspaces.cloudify.usm.USMEventLogger", "-Output]", "-Error]"

		};

		FilteredOutputHandler(final BufferedReader reader, final boolean verbose) {
			this.reader = reader;
			this.verbose = verbose;
		}

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
						for (String filter : FILTERS) {

							if (line.indexOf(filter) >= 0) {
								System.out.println(line);
							}

						}
					}

				} catch (IOException e) {
					return;
				}

			}
		}
	}

	private int executeRecipe(CommandLine cmdLine, Map<Object, Object> env) {
		DefaultExecutor executor = new DefaultExecutor();

		// The watchdog will terminate the process if it does not end within the
		// specified timeout
		int externalProcessTimeout = (this.timeout + EXTERNAL_PROCESS_WATCHDOG_TIMEOUT) * 1000;
		ExecuteWatchdog watchdog = new ExecuteWatchdog(externalProcessTimeout);
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

			Thread thread = new Thread(new FilteredOutputHandler(reader, this.verbose));
			thread.setDaemon(true);
			thread.start();

			PumpStreamHandler psh = new PumpStreamHandler(out, out);
			executor.setStreamHandler(psh);
			result = executor.execute(cmdLine, env);
		} catch (ExecuteException e) {
			logger.log(Level.SEVERE,
					"A problem was encountered while executing the recipe: "
							+ e.getMessage(), e);
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"An IO Exception was encountered while executing the recipe: "
							+ e.getMessage(), e);
			result = -2;
		}

		return result;
	}

	private File createServiceFolder(File packagedRecipe) {
		File serviceFolder = null;
		try {
			serviceFolder = unzipFile(packagedRecipe);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to unzip recipe: "
					+ e.getMessage(), e);
		}
		return serviceFolder;
	}

	private File packageRecipe() throws CLIException {
		if (!recipeFolder.exists()) {
			throw new ErrorStatusException("service_file_doesnt_exist",
					recipeFolder.getAbsolutePath(), this.serviceFileName);
		}

		if (recipeFolder.isFile()) {

			if (recipeFolder.getName().endsWith(".zip")
					|| recipeFolder.getName().endsWith(".jar")) {
				return recipeFolder;
			} else {
				throw new ErrorStatusException("not_jar_or_zip",
						recipeFolder.getAbsolutePath(), this.serviceFileName);
			}

		}

		// it's a folder
		File dslDirOrFile = recipeFolder;

		if (serviceFileName != null) {
			// use non default service file
			dslDirOrFile = new File(dslDirOrFile, serviceFileName);
		}
		return Pack.doPack(dslDirOrFile);
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	private CommandLine createCommandLine() {
		String javaPath = getJavaPath();

		final String gsHome = Environment.getHomeDirectory();
		final String[] COMMAND_LINE = { "-Dcom.gs.home=" + gsHome,
				"-Dorg.hyperic.sigar.path=" + gsHome + "/lib/platform/sigar",
				"-Dcom.gs.usm.RecipeShutdownTimeout=" + timeout,
				IntegratedProcessingUnitContainer.class.getName(), "-cluster",
				"id=1", "total_members=1" };
		CommandLine cmdLine = new CommandLine(javaPath);

		for (String param : COMMAND_LINE) {
			cmdLine.addArgument(param);
		}

		if (this.serviceFileName != null) {
			cmdLine.addArgument("-properties");
			cmdLine.addArgument("embed://"
					+ CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME
					+ "=" + this.serviceFileName);
		}

		// -Dcom.gs.usm.RecipeShutdownTimeout=10

		return cmdLine;
	}

	private String getJavaPath() {
		long pid = SigarHolder.getSigar().getPid();
		try {
			String javaName = SigarHolder.getSigar().getProcExe(pid).getName();
			return javaName;
		} catch (SigarException e) {
			throw new IllegalStateException(
					"Failed to read java path via sigar from current process ("
							+ pid + ")");
		}
	}

	protected static File createTempDir() throws IOException {
		File targetDir = null;
		final String tmpDir = System.getProperty("java.io.tmpdir");
		if (tmpDir == null) {
			throw new IllegalStateException(
					"The java.io.tmpdir property is null. Can't create a temporary directory for service unpacking");
		}
		if (tmpDir.indexOf(" ") >= 0) {

			targetDir = new File("Recipe_Test_Temp_Files" + File.separator
					+ "Test_" + System.currentTimeMillis());
			if (!targetDir.mkdirs()) {
				throw new IllegalStateException(
						"Failed to create a directory where service will be unzipped. Target was: "
								+ targetDir);
			}
			logger.warning("The System temp directory name includes spaces. Using alternate directory: "
					+ targetDir);

		}
		final File tempFile = File.createTempFile("GS_tmp_dir", ".service",
				targetDir);
		final String path = tempFile.getAbsolutePath();
		tempFile.delete();
		tempFile.mkdirs();
		final File baseDir = new File(path);
		return baseDir;
	}

	private static File unzipFile(final File inputFile) throws IOException {

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
				ServiceReader.copyInputStream(zipFile.getInputStream(entry),
						new BufferedOutputStream(new FileOutputStream(file)));
			}
			return baseDir;

		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (final IOException e) {
					logger.log(
							Level.SEVERE,
							"Failed to close zip file after unzipping zip contents",
							e);
				}
			}
		}

	}

	public static final void copyInputStream(final InputStream in,
			final OutputStream out) throws IOException {
		final byte[] buffer = new byte[4096];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}

}
