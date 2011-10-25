package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.openspaces.admin.Admin;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.ui.UserInterface;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Cloud;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;
import com.gigaspaces.cloudify.dsl.internal.packaging.Packager;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.dsl.internal.packaging.ZipUtils;

public class ServiceReader {

	/*****
	 * Private Constructor to prevent instantiation.
	 * 
	 */
	private ServiceReader() {

	}

	/**
	 * receive as parameter 2 variables. first we will check the path given in
	 * the DSL file. If the path exists we will return the jar file. else we
	 * will look for the jar file in the service directory.
	 * 
	 * @param serviceFileOrDir
	 * @param serviceFileName
	 * @return
	 * @throws IOException
	 */
	// public static File getJarFileFromDir(final String serviceFileOrDirPath)
	// throws IOException {
	// File serviceFileOrDir = new File(serviceFileOrDirPath);
	// if (serviceFileOrDir.exists() && serviceFileOrDir.isDirectory()){
	// File jarFile = File.createTempFile("serviceJar", ".jar");
	// ZipUtils.zip(serviceFileOrDir, jarFile);
	// return jarFile;
	// }
	// else if (serviceFileOrDir.exists() && !serviceFileOrDir.isDirectory()){
	// return serviceFileOrDir;
	// }
	//
	// throw new
	// FileNotFoundException("The service deployment file was not found in " +
	// serviceFileOrDir);
	// }

	/**
	 * Checks if the specified file is a recipe folder. If it is returns the
	 * default recipe file. If the specified file is a file, it just returns the
	 * specified file.
	 * 
	 * @throws FileNotFoundException
	 *             - the specified file was not found
	 * @throws PackagingException
	 *             - the specified folder had more or less than 1 recipe file
	 */
	public static File findServiceFile(final File serviceDir)
			throws FileNotFoundException, PackagingException {

		if (!serviceDir.exists()) {
			throw new FileNotFoundException("Cannot find "
					+ serviceDir.getAbsolutePath());
		}

		if (!serviceDir.isDirectory()) {
			throw new IllegalArgumentException(serviceDir.getAbsolutePath()
					+ " must be a directory");
		}

		final File[] files = serviceDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith("-service.groovy");
			}
		});

		if (files.length > 1) {
			throw new PackagingException(
					"Found multiple service configuration files: "
							+ Arrays.toString(files)
							+ ". "
							+ "Only one may be supplied in the ext folder of the PU Jar file.");
		}

		if (files.length == 0) {
			throw new PackagingException(
					"Cannot find service configuration file in "
							+ serviceDir.getAbsolutePath());
		}

		return files[0];

	}

	public static File extractProjectFile(File projectZipFile)
			throws IOException {

		File destFolder = null;
		destFolder = File.createTempFile("gs_usm_", "");
		FileUtils.forceDelete(destFolder);
		FileUtils.forceMkdir(destFolder);

		ZipUtils.unzip(projectZipFile, destFolder);

		return destFolder;

	}

	public static Service readService(File serviceDirOrFile)
			throws IOException, PackagingException {

		File dslFile = serviceDirOrFile;
		if (!dslFile.isFile()) {
			dslFile = ServiceReader.findServiceFile(serviceDirOrFile);
		}

		Service service = getServiceFromFile(dslFile);
		validateFolderSize(serviceDirOrFile, service.getMaxJarSize());
		return getServiceFromFile(dslFile);
	}

	public static void validateFolderSize(File serviceDirOrFile,
			long maxJarSizePermitted) throws PackagingException {
		File folder = serviceDirOrFile;
		if (folder.isFile()) {
			folder = folder.getParentFile();
		}
		final long folderSize = FileUtils.sizeOfDirectory(folder);
		if (folderSize == 0) {
			throw new PackagingException("folder " + folder.getAbsolutePath()
					+ " is empty");
		}
		final long maxJarSize = maxJarSizePermitted;
		if ((folderSize > maxJarSize) || (folderSize == 0)) {
			throw new PackagingException("folder " + folder.getAbsolutePath()
					+ "size is: "
					+ FileUtils.byteCountToDisplaySize(folderSize)
					+ ", it must be smaller than: "
					+ FileUtils.byteCountToDisplaySize(maxJarSize));
		}

	}

	public static Service getServiceFromFile(final File dslFile)
			throws PackagingException {
		try {
			return ServiceReader.getServiceFromFile(dslFile,
			// new File(System.getProperty("user.dir"))).getService();
					dslFile.getParentFile()).getService();
		} catch (final Exception e) {
			throw new PackagingException("Failed to read service from file "
					+ dslFile + ": " + e.getMessage(), e);
		}
	}

	/****************
	 * Reads a service object from a groovy DSL file placed in the given
	 * directory. The file name must be of the format *-service.groovy, and
	 * there must be exactly one file in the directory with a name that matches
	 * this format.
	 * 
	 * @param dir
	 *            the directory to scan for the DSL file.
	 * @return the service
	 * @throws PackagingException
	 * @throws FileNotFoundException
	 */
	// TODO - Incorrect name - should be from Dir
	public static DSLServiceCompilationResult getServiceFromDirectory(
			final File dir) throws FileNotFoundException, PackagingException {
		return getServiceFromFile(findServiceFile(dir), dir);

	}

	public static DSLServiceCompilationResult getServiceFromFile(
			final File dslFile, final File workDir) {
		return ServiceReader.getServiceFromFile(dslFile, workDir, null, null,
				null, true);
	}

	// TODO - consider adding a DSL exception
	public static DSLServiceCompilationResult getServiceFromFile(
			final File dslFile, final File workDir, final Admin admin,
			final ClusterInfo clusterInfo, final String propertiesFileName,
			final boolean isRunningInGSC) {

		LinkedHashMap<Object,Object> properties = null;
		try {
			properties = createServiceProperties(dslFile, workDir,
					propertiesFileName);
		} catch (Exception e) {
			String fileName = propertiesFileName;
			if(fileName == null) {
				fileName = "<default properties file>";
			}
			throw new IllegalArgumentException(
					"Failed to load properties file " + fileName, e);
		}

		// create an uninitialized service context
		final ServiceContext ctx = new ServiceContext();
		// create the groovy shell, loaded with our settings
		final GroovyShell gs = ServiceReader.createGroovyShellForService(
				properties, ctx);

		Object result = evaluateGroovyScript(dslFile, gs);

		if (result == null) {
			throw new IllegalStateException("The file " + dslFile
					+ " evaluates to null, not to a service object");
		}
		if (!(result instanceof Service)) {
			throw new IllegalStateException("The file: " + dslFile
					+ " did not evaluate to the required object type");
		}

		final Service service = (Service) result;

		if (isRunningInGSC) {
			ctx.init(service, admin, workDir.getAbsolutePath(), clusterInfo);
		} else {
			ctx.initInIntegratedContainer(service, workDir.getAbsolutePath());
		}
		return new DSLServiceCompilationResult(service, ctx, dslFile);
	}

	private static Object evaluateGroovyScript(final File dslFile,
			final GroovyShell gs) {
		// Evaluate class using a FileReader, as the *-service files create a
		// class with an illegal name
		Object result = null;
		FileReader reader = null;
		try {
			reader = new FileReader(dslFile);
			result = gs.evaluate(reader, "service");
		} catch (final CompilationFailedException e) {
			throw new IllegalArgumentException("The file " + dslFile
					+ " could not be compiled", e);
		} catch (final IOException e) {
			throw new IllegalStateException("The file " + dslFile
					+ " could not be read", e);
		} catch (MissingMethodException e) {
			throw new IllegalArgumentException(
					"Could not resolve DSL entry with name: " + e.getMethod(), e);
		} catch (MissingPropertyException e) {
			throw new IllegalArgumentException(
					"Could not resolve DSL entry with name: " + e.getProperty(),
					e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<Object, Object> createServiceProperties(
			final File dslFile, final File workDir,
			final String propertiesFileName) throws IOException {

		// TODO - refactor this - remove the recursive call.
		if (propertiesFileName != null) {
			File propertiesFile = new File(workDir, propertiesFileName);
			if (!propertiesFile.exists()) {
				throw new FileNotFoundException("Could not find file: "
						+ propertiesFileName + " in directory: " + workDir);
			}

			ConfigObject config = new ConfigSlurper().parse(propertiesFile
					.toURI().toURL());

			return config;		

		} else {
			String baseFileName = dslFile.getName();
			String[] parts = baseFileName.split(Pattern.quote("."));
			if (parts.length > 0) {
				baseFileName = parts[0];
			}
			String defaultPropertiesFileName = baseFileName + ".properties";
			String actualPropertiesFileName = defaultPropertiesFileName;
			try {
				return createServiceProperties(dslFile, workDir,
						actualPropertiesFileName);
			} catch (FileNotFoundException e) {
				return new LinkedHashMap<Object, Object>();
			}

		}
	}

	private static GroovyShell createGroovyShellForService(
			LinkedHashMap<Object, Object> properties, ServiceContext context) {
		return ServiceReader.createGroovyShell(
				BaseServiceScript.class.getName(), properties, context);
	}

	private static GroovyShell createGroovyShellForApplication() {
		return ServiceReader.createGroovyShell(
				BaseApplicationScript.class.getName(), null, null);
	}

	private static GroovyShell createGroovyShellForCloud() {
		return ServiceReader.createGroovyShell(BaseDslScript.class.getName(),
				null, null);
	}

	private static GroovyShell createGroovyShell(final String baseClassName,
			final LinkedHashMap<Object,Object> properties, ServiceContext context) {
		final CompilerConfiguration cc = ServiceReader
				.createCompilerConfiguration(baseClassName);

		final Binding binding = createGroovyBinding(properties, context);

		final GroovyShell gs = new GroovyShell(
				ServiceReader.class.getClassLoader(), // this.getClass().getClassLoader(),
				binding, cc);
		return gs;
	}

	private static Binding createGroovyBinding(final LinkedHashMap<Object,Object> properties,
			ServiceContext context) {
		final Binding binding = new Binding();
		if (properties != null) {
			Set<Entry<Object, Object>> entries = properties.entrySet();
			for (Entry<Object, Object> entry : entries) {
				binding.setVariable((String) entry.getKey(),
						entry.getValue());
			}
			if (context != null) {
				binding.setVariable("context", context);
			}
		}
		return binding;
	}

	private static CompilerConfiguration createCompilerConfiguration(
			final String baseClassName) {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(com.gigaspaces.cloudify.dsl.Service.class
				.getPackage().getName(), UserInterface.class.getPackage()
				.getName(), com.gigaspaces.cloudify.dsl.context.Service.class
				.getPackage().getName());

		ic.addImports(com.gigaspaces.cloudify.dsl.utils.ServiceUtils.class
				.getName());

		cc.addCompilationCustomizers(ic);

		cc.setScriptBaseClass(baseClassName);

		return cc;
	}

	// TODO - Support Zip files in application
	public static Cloud getCloudFromDirectory(final File serviceFolder)
			throws IOException {

		File actualDslFile = getCloudDSLFileFromDirectory(serviceFolder);

		if (actualDslFile == null) {
			return null;
		}
		final Cloud cloud = ServiceReader.readCloudFromFile(actualDslFile);

		// execute post processing step to generate service objects

		// final File appDir = dslFile.getParentFile();
		// app.generateServices(appDir);
		//
		// final List<Service> services = new ArrayList<Service>(
		// serviceNames.size());
		// for (final String serviceName : serviceNames) {
		// final Service service = ServiceReader.readApplicationService(app,
		// serviceName, appDir).getService();
		// services.add(service);
		// }

		// app.setServices(services);

		return cloud;

	}

	// TODO - Support Zip files in application
	public static DSLApplicationCompilatioResult getApplicationFromFile(
			final File inputFile) throws IOException {

		File actualApplicationDslFile = inputFile;

		if (inputFile.isFile()) {
			if (inputFile.getName().endsWith(".zip")
					|| inputFile.getName().endsWith(".jar")) {

				// Unzip application zip file to temp folder
				actualApplicationDslFile = ServiceReader
						.unzipApplicationFile(inputFile);
			}
		}
		final File dslFile = ServiceReader
				.getApplicationDslFile(actualApplicationDslFile);

		final Application app = ServiceReader.readApplicationFromFile(dslFile);

		// execute post processing step to generate service objects

		// final File appDir = dslFile.getParentFile();
		// app.generateServices(appDir);
		//
		// final List<Service> services = new ArrayList<Service>(
		// serviceNames.size());
		// for (final String serviceName : serviceNames) {
		// final Service service = ServiceReader.readApplicationService(app,
		// serviceName, appDir).getService();
		// services.add(service);
		// }

		// app.setServices(services);

		return new DSLApplicationCompilatioResult(app,
				actualApplicationDslFile.getParentFile(),
				actualApplicationDslFile);

	}

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceReader.class.getName());

	private static File unzipApplicationFile(final File inputFile)
			throws IOException {

		ZipFile zipFile = null;
		try {
			final File baseDir = ServiceReader.createTempDir();
			zipFile = new ZipFile(inputFile);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();

				if (entry.isDirectory()) {

					logger.finer("Extracting directory: " + entry.getName());
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
			return ServiceReader.getApplicationDSLFileFromDirectory(baseDir);

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

	public static Service readServiceFromZip(final File inputFile)
			throws IOException, PackagingException {
		File projectFolder = extractProjectFile(inputFile);
		try {
			return ServiceReader.getServiceFromDirectory(projectFolder)
					.getService();
		} finally {
			FileUtils.forceDelete(projectFolder);
		}

	}

	public static final void copyInputStream(final InputStream in,
			final OutputStream out) throws IOException {
		final byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}

	protected static File createTempDir() throws IOException {
		final File tempFile = File.createTempFile("GS_tmp_dir", ".application");
		final String path = tempFile.getAbsolutePath();
		tempFile.delete();
		tempFile.mkdirs();
		final File baseDir = new File(path);
		return baseDir;
	}

	private static File getApplicationDslFile(final File inputFile)
			throws FileNotFoundException {
		if (!inputFile.exists()) {
			throw new FileNotFoundException("Could not find file: " + inputFile);
		}

		if (inputFile.isFile()) {
			if (inputFile.getName().endsWith("-application.groovy")) {
				return inputFile;
			}
			if (inputFile.getName().endsWith(".zip")
					|| inputFile.getName().endsWith(".jar")) {
				return ServiceReader.getApplicationDslFileFromZip(inputFile);
			}
		}

		if (inputFile.isDirectory()) {
			return ServiceReader.getApplicationDSLFileFromDirectory(inputFile);
		}

		throw new IllegalStateException("Could not find File: " + inputFile);

	}

	protected static File getApplicationDSLFileFromDirectory(final File dir) {
		final File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return (name.endsWith("-application.groovy"));
			}
		});

		if (files.length != 1) {
			throw new IllegalArgumentException(
					"Expected to find one application file, found "
							+ files.length);
		}

		return files[0];
	}

	protected static File getCloudDSLFileFromDirectory(final File dir) {
		final File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return (name.endsWith("-cloud.groovy"));
			}
		});

		if (files.length == 0) {
			return null;
		}

		if (files.length != 1) {
			throw new IllegalArgumentException(
					"Expected to find one cloud file, found " + files.length);
		}

		return files[0];
	}

	/********
	 * Given an a
	 * 
	 * @param inputFile
	 * @return
	 */
	private static File getApplicationDslFileFromZip(final File inputFile) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Application readApplicationFromFile(final File dslFile)
			throws IOException {

		if (!dslFile.exists()) {
			throw new FileNotFoundException(dslFile.getAbsolutePath());
		}

		final GroovyShell gs = ServiceReader.createGroovyShellForApplication();
		gs.getContext().setProperty(DSLUtils.APPLICATION_DIR,
				dslFile.getParentFile().getAbsolutePath());

		Object result = null;
		FileReader reader = null;
		try {
			reader = new FileReader(dslFile);
			result = gs.evaluate(reader, "application");
		} catch (final CompilationFailedException e) {
			throw new IllegalArgumentException("The file " + dslFile
					+ " could not be compiled", e);
		} catch (final IOException e) {
			throw new IllegalStateException("The file " + dslFile
					+ " could not be read", e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		// final Object result = Eval.me(expr);
		if (result == null) {
			throw new IllegalStateException("The file: " + dslFile
					+ " evaluates to null, not to an application object");
		}
		if (!(result instanceof Application)) {
			throw new IllegalStateException("The file: " + dslFile
					+ " did not evaluate to the required object type");
		}

		final Application application = (Application) result;

		// final ServiceContext ctx = new ServiceContext(service, admin,
		// workDir.getAbsolutePath(),
		// clusterInfo);
		// gs.getContext().setProperty("context", ctx);

		return application;

	}

	private static Cloud readCloudFromFile(final File dslFile)
			throws IOException {

		if (!dslFile.exists()) {
			throw new FileNotFoundException(dslFile.getAbsolutePath());
		}

		final GroovyShell gs = ServiceReader.createGroovyShellForCloud();
		// gs.getContext().setProperty(ServiceUtils.APPLICATION_DIR,
		// dslFile.getParentFile().getAbsolutePath());

		Object result = null;
		FileReader reader = null;
		try {
			reader = new FileReader(dslFile);
			result = gs.evaluate(reader, "cloud");
		} catch (final CompilationFailedException e) {
			throw new IllegalArgumentException("The file " + dslFile
					+ " could not be compiled", e);
		} catch (final IOException e) {
			throw new IllegalStateException("The file " + dslFile
					+ " could not be read", e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		// final Object result = Eval.me(expr);
		if (result == null) {
			throw new IllegalStateException("The file: " + dslFile
					+ " evaluates to null, not to a DSL object");
		}
		if (!(result instanceof Cloud)) {
			throw new IllegalStateException("The file: " + dslFile
					+ " did not evaluate to the required object type");
		}

		final Cloud cloud = (Cloud) result;

		// final ServiceContext ctx = new ServiceContext(service, admin,
		// workDir.getAbsolutePath(),
		// clusterInfo);
		// gs.getContext().setProperty("context", ctx);

		return cloud;

	}

	public static File doPack(File recipeFolder) throws IOException,
			PackagingException {
		Service service = ServiceReader.readService(recipeFolder);
		File packedFile = Packager.pack(recipeFolder, service);
		return packedFile;
	}

}
