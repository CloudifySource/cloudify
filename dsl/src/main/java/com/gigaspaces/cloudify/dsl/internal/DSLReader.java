package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

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
import java.util.Map.Entry;
import java.util.Properties;
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
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;
import com.gigaspaces.cloudify.dsl.internal.packaging.Packager;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.dsl.internal.packaging.ZipUtils;

/*******
 * This class is a work in progress. DO NOT USE IT!
 * @author barakme
 *
 */

public class DSLReader {

	private File workFolder;
	private Properties properties;
	private GroovyShell gs;
	private boolean runningInGSC;
	private ClusterInfo clusterInfo;
	private Admin admin;
	private ServiceContext context;
	public DSLReader(File workFolder, Properties properties, boolean runningInGSC, ClusterInfo clusterInfo, Admin admin) {

		this.workFolder = workFolder;
		this.properties = properties;
		gs = createGroovyShell(properties);
		this.runningInGSC = runningInGSC;
		this.clusterInfo = clusterInfo;
		this.admin = admin;
		// create an uninitialized service context
		this.context = new ServiceContext();
		
	}
	
	public void processServiceFolder() throws FileNotFoundException {
		Application application = loadApplication(true);
	}
	
	private Application loadApplication(final boolean isServiceFolder) throws FileNotFoundException {
		File applicationDslFile = getApplicationFile();
		if(applicationDslFile == null) {
			// must be a singleton service
			
		}
				

//		final File dslFile = ServiceReader
//				.getApplicationDslFile(actualApplicationDslFile);
//
//		final Application app = ServiceReader.readApplicationFromFile(dslFile);

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

//		return new DSLApplicationCompilatioResult(app,
//				actualApplicationDslFile.getParentFile(),
//				actualApplicationDslFile);

		// TODO Auto-generated method stub
		return null;
	}

	private File getApplicationFile() throws FileNotFoundException {
		return getDSLFile("-application.groovy", CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_FILE_NAME);
	}
	
	private File getServiceFile() throws FileNotFoundException {
		return getDSLFile("-service.groovy", CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME);
	}
	
	private File getCloudFile() throws FileNotFoundException {
		return getDSLFile("-cloud.groovy", CloudifyConstants.CONTEXT_PROPERTY_CLOUD_FILE_NAME);
	}
	
	private File getDSLFile(final String fileNameSuffix, final String propertyKey) throws FileNotFoundException {
		if(propertyKey != null) {
			final String providedFileName = properties.getProperty(propertyKey);
			if(providedFileName != null){
				File providedFile = new File(this.workFolder, providedFileName);
				if(providedFile.exists()) {
					return providedFile;
				} else {
					throw new FileNotFoundException("Could not find DSL file with provided name: " + providedFileName);
				}
			}
		}
		
		File[] matchingFiles = this.workFolder.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(fileNameSuffix);
			}
		});
		
		if(matchingFiles.length == 0){
			return null;
		}
		if(matchingFiles.length > 1) {
			throw new IllegalStateException("Found multiple DSL files with the suffix " + fileNameSuffix +". Only one may be provided");
		}
		return matchingFiles[0];
		
	}
	
	public Object readDSLObject(
			final File dslFile) {

		// create the groovy shell, loaded with our settings
		
		Object result = evaluateGroovyScript(dslFile, gs);
		
		if (result == null) {
			throw new IllegalStateException("The file " + dslFile
					+ " evaluates to null, not a DSL object");
		}
//		if (!(result instanceof Service)) {
//			throw new IllegalStateException("The file: " + dslFile
//					+ " did not evaluate to the required object type");
//		}
//
//		final Service service = (Service) result;
//
//		if (runningInGSC) {
//			context.init(service, admin, serviceFolder.getAbsolutePath(),
//					clusterInfo);
//		} else {
//			context.initInIntegratedContainer(service, serviceFolder.getAbsolutePath());
//		}
		
		return result;
	}

	
	/**
	 * Checks if the specified file is a recipe folder. If it is returns the default recipe file.
	 * If the specified file is a file, it just returns the specified file.
	 * 
	 * @throws FileNotFoundException - the specified file was not found
	 * @throws PackagingException - the specified folder had more or less than 1 recipe file
	 */
	public static File findServiceFile(final File serviceDir) throws FileNotFoundException, PackagingException {
		
		if (!serviceDir.exists()) {
			throw new FileNotFoundException("Cannot find " + serviceDir.getAbsolutePath());
		}
		
		if (!serviceDir.isDirectory()) {
			throw new IllegalArgumentException(serviceDir.getAbsolutePath() + " must be a directory");
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
			throw new PackagingException("Cannot find service configuration file in " + serviceDir.getAbsolutePath());
		}
		
		return files[0];

	}

	public static File extractProjectFile(File projectZipFile) throws IOException{
		
		File destFolder = null;
		destFolder = File.createTempFile("gs_usm_", "");
		FileUtils.forceDelete(destFolder);
		FileUtils.forceMkdir(destFolder);
		
		ZipUtils.unzip(projectZipFile, destFolder);
		
		return destFolder;
		
	}

	public static Service readService(File serviceDirOrFile) throws IOException, PackagingException {
		
		File dslFile = serviceDirOrFile;
		if (!dslFile.isFile()) {
			dslFile = findServiceFile(serviceDirOrFile);
		}
		
		Service service = getServiceFromFile(dslFile);
		validateFolderSize(serviceDirOrFile, service.getMaxJarSize());
		return getServiceFromFile(dslFile);
	}
	
	public static void validateFolderSize(File serviceDirOrFile, long maxJarSizePermitted) throws PackagingException {
		File folder = serviceDirOrFile;
		if (folder.isFile()) {
			folder = folder.getParentFile();
		}
		final long folderSize = FileUtils.sizeOfDirectory(folder);
		if (folderSize == 0) {
			throw new PackagingException("folder " + folder.getAbsolutePath() + " is empty");
		}
		final long maxJarSize = maxJarSizePermitted;
		if ((folderSize > maxJarSize) || (folderSize == 0)) {
			throw new PackagingException("folder " + folder.getAbsolutePath() + "size is: "
					+ FileUtils.byteCountToDisplaySize(folderSize)
					+ ", it must be smaller than: " + FileUtils.byteCountToDisplaySize(maxJarSize));
		}
		
	}
	
	public static Service getServiceFromFile(final File dslFile) throws PackagingException {
			return null;
//		try {
//			return getServiceFromFile(dslFile, new File(System.getProperty("user.dir"))).getService();
//		} catch (final Exception e) {
//			throw new PackagingException("Failed to read service from file " + dslFile + ": " + e.getMessage(), e);
//		}
	}
	
	private Object evaluateGroovyScript(final File dslFile,
			final GroovyShell gs) {
		// Evaluate class using a FileReader, as the *-service files create a class with an illegal name
		// and give each compiled class a unique name
		Object result = null;
		FileReader  reader = null;
		try {
			reader = new FileReader(dslFile);
			result = gs.evaluate(reader, "groovyScript" + System.currentTimeMillis());
		} catch (final CompilationFailedException e) {
			throw new IllegalArgumentException("The file " + dslFile
					+ " could not be compiled", e);
		} catch (final IOException e) {
			throw new IllegalStateException("The file " + dslFile
					+ " could not be read", e);
		}
		finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	private static Properties createServiceProperties(final File dslFile,
			final File workDir, final String propertiesFileName)
	throws IOException {
		Properties properties = new Properties();
		if (propertiesFileName != null) {
			File propertiesFile = new File(workDir, propertiesFileName);
			if (!propertiesFile.exists()) {
				throw new FileNotFoundException("Could not find file: "
						+ propertiesFileName + " in directory: " + workDir);
			}
			FileReader reader = null;
			try {
				reader = new FileReader(propertiesFile);
				properties.load(reader);

			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
						logger.log(Level.SEVERE,
								"Failed to close properties file reader", e1);
					}
				}
			}

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
				// ignore - no properties;
			}

		}
		return properties;
	}



	private GroovyShell createGroovyShell(final Properties properties) {
		final CompilerConfiguration cc = createCompilerConfiguration();

		final Binding binding = createGroovyBinding(properties, context);

		final GroovyShell gs = new GroovyShell(
				ServiceReader.class.getClassLoader(), // this.getClass().getClassLoader(),
				binding, cc);
		return gs;
	}
	
	private CompilerConfiguration createCompilerConfiguration() {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(com.gigaspaces.cloudify.dsl.Service.class
				.getPackage().getName(),
				UserInterface.class.getPackage()
				.getName(),
				com.gigaspaces.cloudify.dsl.context.Service.class.getPackage()
				.getName()
				);
		
				

		cc.addCompilationCustomizers(ic);

		cc.setScriptBaseClass(BaseDslScript.class.getName());

		return cc;
	}


	private static Binding createGroovyBinding(final Properties properties, ServiceContext context) {
		final Binding binding = new Binding();
		if (properties != null) {
			Set<Entry<Object, Object>> entries = properties.entrySet();
			for (Entry<Object, Object> entry : entries) {
				binding.setVariable((String) entry.getKey(),
						(String) entry.getValue());
			}
			if(context != null) {
				binding.setVariable("context", context);
			}
		}
		return binding;
	}


	// TODO - Support Zip files in application
	public static DSLApplicationCompilatioResult getApplicationFromFile(final File inputFile)
	throws IOException {

//		File actualApplicationDslFile = inputFile;
//
//		if (inputFile.isFile()) {
//			if (inputFile.getName().endsWith(".zip")
//					|| inputFile.getName().endsWith(".jar")) {
//
//				// Unzip application zip file to temp folder
//				actualApplicationDslFile = ServiceReader
//				.unzipApplicationFile(inputFile);
//			}
//		}
//		final File dslFile = ServiceReader
//		.getApplicationDslFile(actualApplicationDslFile);
//
//		final Application app = readApplicationFromFile(dslFile);
//
//		// execute post processing step to generate service objects
//
//		//		final File appDir = dslFile.getParentFile();
//		//		app.generateServices(appDir);
//		//				
//		//		final List<Service> services = new ArrayList<Service>(
//		//				serviceNames.size());
//		//		for (final String serviceName : serviceNames) {
//		//			final Service service = readApplicationService(app,
//		//					serviceName, appDir).getService();
//		//			services.add(service);
//		//		}
//
//		//		app.setServices(services);
//
//		return new DSLApplicationCompilatioResult(app, actualApplicationDslFile.getParentFile(), actualApplicationDslFile);

		return null;

	}

	private static java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(DSLReader.class.getName());

	private static File unzipApplicationFile(final File inputFile)
	throws IOException {

		ZipFile zipFile = null;
		try {
			final File baseDir = createTempDir();
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
				copyInputStream(zipFile.getInputStream(entry),
						new BufferedOutputStream(new FileOutputStream(file)));
			}
			return getApplicationDSLFileFromDirectory(baseDir);

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
	
	
//	public static Service readServiceFromZip(final File inputFile)
//	throws IOException, PackagingException {
//		File projectFolder = extractProjectFile(inputFile);
//		try{
//			return getServiceFromDirectory(projectFolder).getService();
//		}finally{
//			FileUtils.forceDelete(projectFolder);
//		}
//		
//	}

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
				return getApplicationDslFileFromZip(inputFile);
			}
		}

		if (inputFile.isDirectory()) {
			return getApplicationDSLFileFromDirectory(inputFile);
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

	public static File doPack(File recipeFolder)
	throws IOException, PackagingException {
		Service service = readService(recipeFolder);
		File packedFile = Packager.pack(recipeFolder, service);
		return packedFile;
	}


}

