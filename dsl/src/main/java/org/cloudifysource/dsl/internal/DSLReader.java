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
package org.cloudifysource.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.RemoteExecutionModes;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.domain.context.BaseServiceContext;
import org.cloudifysource.domain.context.ServiceContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/*******
 * Generic Cloudify DSL Reader.
 * 
 * @author barakme
 * 
 */

public class DSLReader {

	public DSLReader() {

	}

	// Groovy DSL prefix, used for handling print and println correctly
	private static final String GROOVY_SERVICE_PREFIX =
			"Object.metaClass.println = {x->this.println(x)}; Object.metaClass.print =  {x->this.print(x)};";
	/*****
	 * Name of the logger used to process dsl print/println statements.
	 */
	public static final String DSL_LOGGER_NAME = "dslLogger";
	private static Logger logger = Logger.getLogger(DSLReader.class.getName());
	private static Logger dslLogger = Logger.getLogger(DSL_LOGGER_NAME);

	private boolean loadUsmLib = true;

	private ServiceContext context;

	private String propertiesFileName;

	private boolean isRunningInGSC;

	private File dslFile;
	private File workDir;

	private String dslName;
	private String dslFileNamePrefix;
	private String dslFileNameSuffix;

	private File propertiesFile;
	private File overridesFile;

	private boolean createServiceContext = true;

	private final Map<String, Object> bindingProperties = new HashMap<String, Object>();
	private final Map<String, Object> overrideProperties = new HashMap<String, Object>();
	private final Map<String, Object> overrideFields = new HashMap<String, Object>();
	private Map<String, Object> applicationProperties;

	private String dslContents;

	private boolean validateObjects = true;

	private String overridesScript = null;

	/*******
	 * Variables injected into the context of a groovy compilation (binding) Used with the service extension mechanism
	 * to pass defined properties, and the context, to the compilation of the parent service.
	 */
	private Map<Object, Object> variables;

	private GroovyClassLoader dslClassLoader;
	private final Object dslSingleton = new Object();

	private static final String[] STAR_IMPORTS = new String[] {
			org.cloudifysource.domain.Service.class.getPackage().getName(),
			FileTransferModes.class.getName(),
			RemoteExecutionModes.class.getName(),
			ScriptLanguages.class.getName() };

	private static final String[] CLASS_IMPORTS = new String[] {
			org.cloudifysource.dsl.utils.ServiceUtils.class.getName(),
			FileTransferModes.class.getName(),
			RemoteExecutionModes.class.getName(),
			ScriptLanguages.class.getName()
			// ,
			// "org.cloudifysource.debug.DebugHook"
	};

	/******
	 * Property name of injected dsl file path.
	 */
	public static final String DSL_FILE_PATH_PROPERTY_NAME = "dslFilePath";
	/*******
	 * Property name of injected validation activation flag.
	 */
	public static final String DSL_VALIDATE_OBJECTS_PROPERTY_NAME = "validateObjectsFlag";

	private void initDslFile()
			throws FileNotFoundException {
		if (dslFile != null) {
			if (workDir == null) {
				workDir = dslFile.getParentFile();
			}
			return;
		}

		if (dslContents != null) {
			return;
		}

		if (workDir == null) {
			throw new IllegalArgumentException("both dslFile and workDir are null");
		}

		if (this.dslFileNameSuffix == null) {
			throw new IllegalArgumentException("dslFileName suffix has not been set");
		}

		if (!workDir.exists()) {
			throw new FileNotFoundException("Cannot find " + workDir.getAbsolutePath());
		}

		if (!workDir.isDirectory()) {
			throw new IllegalArgumentException(workDir.getAbsolutePath() + " must be a directory");
		}

		dslFile = findDefaultDSLFile(dslFileNameSuffix, workDir);
		if (workDir == null) {
			workDir = dslFile.getParentFile();
		}

	}

	/***********
	 * Search the directory for a file with the specified suffix. Assuming there is exactly one file with that suffix in
	 * the directory.
	 * 
	 * @param fileNameSuffix
	 *            The suffix.
	 * @param dir
	 *            The directory.
	 * @return the file.
	 */
	public static File findDefaultDSLFile(final String fileNameSuffix, final File dir) {

		final File[] files = findDefaultDSLFiles(fileNameSuffix, dir);

		if (files == null || files.length == 0) {
			throw new IllegalArgumentException("Cannot find configuration file in " + dir.getAbsolutePath() + "/*"
					+ fileNameSuffix);
		}
		if (files.length > 1) {
			throw new IllegalArgumentException("Found multiple configuration files: " + Arrays.toString(files) + ". "
					+ "Only one may be supplied in the folder.");
		}

		return files[0];
	}

	/***********
	 * Search the directory for files with the specified suffix.
	 * 
	 * @param fileNameSuffix
	 *            The suffix.
	 * @param directory
	 *            The directory to search at.
	 * @return The found files. Returns null if no file with the specified suffix was found.
	 */
	public static File[] findDefaultDSLFiles(final String fileNameSuffix, final File directory) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory.");
		}

		final File[] files = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(fileNameSuffix);
			}
		});
		if (files.length == 0) {
			return null;
		}

		return files;
	}

	/**
	 * 
	 * @param fileNameSuffix
	 *            .
	 * @param dir
	 *            .
	 * @return The found file or null.
	 */
	public static File findDefaultDSLFileIfExists(
			final String fileNameSuffix, final File dir) {
		File found = null;
		try {
			found = findDefaultDSLFile(fileNameSuffix, dir);
		} catch (final IllegalArgumentException e) {
			if (e.getMessage().contains("Found multiple configuration files")) {
				throw e;
			}
		}
		return found;
	}

	private void init()
			throws IOException {
		initDslFile();
		setDslName();
		initPropertiesFile();
		initOverridesFile();
	}

	private void initOverridesFile() throws IOException {
		overridesFile = getFileIfExist(overridesFile, dslFileNamePrefix + DSLUtils.OVERRIDES_FILE_SUFFIX);
	}

	private static void createDSLOverrides(final File file, final String script,
			final Map<String, Object> overridesMap)
			throws IOException {
		if (file == null && script == null) {
			return;
		}
		if (file != null) {
			try {
				final ConfigObject parse = new ConfigSlurper().parse(file.toURI().toURL());
				parse.flatten(overridesMap);
			} catch (final Exception e) {
				throw new IOException("Failed to read overrides file: " + file, e);
			}
		}
		if (script != null) {
			final ConfigObject parse = new ConfigSlurper().parse(script);
			parse.flatten(overridesMap);
		}
	}

	private Map<String, Object> createApplicationProperties() throws IOException {
		final File externalPropertiesFile = getFileIfExist(null, DSLUtils.APPLICATION_PROPERTIES_FILE_NAME);
		final Map<String, Object> externalProperties = new HashMap<String, Object>();
		createDSLOverrides(externalPropertiesFile, null, externalProperties);
		final File externalOverridesFile = getFileIfExist(null, DSLUtils.APPLICATION_OVERRIDES_FILE_NAME);
		final Map<String, Object> externalOverrides = new HashMap<String, Object>();
		createDSLOverrides(externalOverridesFile, null, externalOverrides);
		if (externalOverrides != null) {
			for (final Entry<String, Object> entry : externalOverrides.entrySet()) {
				externalProperties.put(entry.getKey(), entry.getValue());
			}
		}
		return externalProperties;
	}

	/*********
	 * Executes the current DSL reader, returning the required Object type.
	 * 
	 * @param clazz
	 *            the expected class type returned from the DSL file.
	 * @param <T>
	 *            The Class type returned from this type of DSL file.
	 * @return the domain POJO.
	 * @throws DSLException
	 *             in case there was a problem processing the DSL file.
	 */
	public <T> T readDslEntity(final Class<T> clazz)
			throws DSLException {

		final Object result = readDslObject();
		if (result == null) {
			throw new IllegalStateException("The file " + dslFile + " evaluates to null, not to a DSL object");
		}
		if (!clazz.isAssignableFrom(result.getClass())) {
			throw new IllegalStateException("The file: " + dslFile + " did not evaluate to the required object type");
		}

		@SuppressWarnings("unchecked")
		final T resultObject = (T) result;
		return resultObject;
	}

	private Object readDslObject()
			throws DSLException {
		try {
			init();
		} catch (final IOException e) {
			throw new DSLException("Failed to initialize DSL Reader: " + e.getMessage(), e);
		}

		LinkedHashMap<Object, Object> properties = null;
		try {
			properties = createDSLProperties();
			createDSLOverrides(overridesFile, overridesScript, overrideProperties);
			overrideProperties(properties);
			addApplicationProperties(properties);
		} catch (final Exception e) {
			// catching exception here, as groovy config slurper may throw just
			// about anything
			String msg = null;
			if (propertiesFile != null) {
				msg = e.getMessage();
			} else {
				msg = "Failed to load properties file: " + e.getMessage();
			}
			throw new IllegalArgumentException(msg, e);
		}

		if (this.variables != null) {
			properties.putAll(this.variables);
		}

		// create an uninitialized service context
		if (this.createServiceContext) {
			String canonicalPath = null;
			try {
				canonicalPath = workDir.getCanonicalPath();
			} catch (final IOException e) {
				throw new DSLException("Failed to get canonical path of work directory: " + workDir + ". Error was: "
						+ e.getMessage(), e);
			}
			if (this.context == null) {
				this.context = new BaseServiceContext(canonicalPath);
			}
		}

		// create the groovy shell, loaded with our settings
		final GroovyShell gs = createGroovyShell(properties);
		final Object result = evaluateGroovyScript(gs);

		if (result == null) {
			throw new DSLException("The DSL evaluated to a null - check your syntax and try again");
		}

		if (this.createServiceContext) {
			if (!(result instanceof Service)) {
				throw new IllegalArgumentException(
						"The DSL reader cannot create a service context to a DSL that does not evaluate to a Service. "
								+ "Set the 'createServiceContext' option to false if you do not need a service conext");
			}

			((BaseServiceContext) this.context).init((Service) result);
		}

		this.dslClassLoader = gs.getClassLoader();

		// The call below is required to clear cached class entries. Without it, a PermGen error will eventually occur.
		// A synchronized block may be required as this call MAY not be thread safe. 
		// More info available here: http://jira.codehaus.org/browse/GROOVY-5121
		synchronized (dslSingleton) {
			// Tell Groovy we don't need any meta
			// information about the generated DSL classes
			GroovySystem.getMetaClassRegistry().removeMetaClass(Object.class);
			// Tell the loader to clear out it's cache,
			// this ensures the classes will be GC'd
			gs.resetLoadedClasses();
		}
		return result;

	}

	/**
	 * 
	 * @param properties
	 *            the properties to add to
	 * @throws IOException
	 */
	private void addApplicationProperties(final Map<Object, Object> properties) throws IOException {
		if (applicationProperties == null) {
			applicationProperties = createApplicationProperties();
		}
		for (final Entry<String, Object> entry : applicationProperties.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 
	 * @param properties
	 *            the properties to override
	 */
	private void overrideProperties(final LinkedHashMap<Object, Object> properties) {
		for (final Entry<String, Object> entry : overrideProperties.entrySet()) {
			final String key = entry.getKey();
			final Object propertyValue = entry.getValue();
			// overrides existing property or add a new one.
			properties.put(key, propertyValue);
		}
	}

	@SuppressWarnings("deprecation")
	private Object evaluateGroovyScript(final GroovyShell gs)
			throws DSLValidationException {
		// Evaluate class using a FileReader, as the *-service files create a
		// class with an illegal name
		Object result = null;

		if (this.dslContents == null) {

			// FileReader reader = null;
			SequenceInputStream sis = null;
			FileInputStream fis = null;
			try {

				fis = new FileInputStream(dslFile);
				final ByteArrayInputStream bis =
						new ByteArrayInputStream(GROOVY_SERVICE_PREFIX.getBytes());
				sis = new SequenceInputStream(bis, fis);
				// reader = new FileReader(dslFile);
				// using a deprecated method here as we do not have a multireader in the dependencies
				// and not really worth another jar just for this.
				result = gs.evaluate(sis, "dslEntity");
			} catch (final IOException e) {
				throw new IllegalStateException("The file " + dslFile + " could not be read", e);
			} catch (final MissingMethodException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getMethod(), e);
			} catch (final MissingPropertyException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getProperty(), e);
			} catch (final DSLValidationRuntimeException e) {
				throw e.getDSLValidationException();
			} catch (final CompilationFailedException e) {
				throw new IllegalArgumentException("Could not parse " + dslFile + ": " + e.getMessage(), e);
			} finally {
				if (sis != null) {
					try {
						sis.close();
					} catch (final IOException e) {
						// ignore
					}
				}
				if (fis != null) {
					try {
						fis.close();
					} catch (final IOException e) {
						// ignore
					}
				}
			}
		} else {
			try {
				result = gs.evaluate(this.dslContents, "dslEntity");
			} catch (final CompilationFailedException e) {
				throw new IllegalArgumentException("The file " + dslFile + " could not be compiled", e);
			}

		}

		return result;
	}

	private void initPropertiesFile()
			throws IOException {
		if (this.propertiesFileName != null) {
			this.propertiesFile = new File(workDir, this.propertiesFileName);

			if (!propertiesFile.exists()) {
				throw new FileNotFoundException("Could not find properties file: " + propertiesFileName);
			}
			if (!propertiesFile.isFile()) {
				throw new FileNotFoundException(propertiesFileName + " is not a file!");
			}

			return;

		}

		if (this.dslFile == null) {
			return;
		}
		// look for default properties file
		// using format <dsl file name>.properties
		final String defaultPropertiesFileName = dslFileNamePrefix + DSLUtils.PROPERTIES_FILE_SUFFIX;

		final File defaultPropertiesFile = new File(workDir, defaultPropertiesFileName);

		if (defaultPropertiesFile.exists()) {
			this.propertiesFileName = defaultPropertiesFileName;
			this.propertiesFile = defaultPropertiesFile;
		}

	}

	private File getFileIfExist(final File file, final String defaultFileName)
			throws IOException {
		if (file != null) {
			if (!file.exists()) {
				throw new FileNotFoundException("Could not find overrides file: "
						+ file.getAbsolutePath());
			}
			if (!file.isFile()) {
				throw new FileNotFoundException(this.overridesFile.getName() + " is not a file!");
			}
			return file;
		}
		if (this.dslFile == null) {
			return null;
		}
		// look for default properties file
		// using format <dsl file name>.suffix
		final File defaultOverridesFile = new File(workDir, defaultFileName);
		if (defaultOverridesFile.exists()) {
			return defaultOverridesFile;
		}

		return null;
	}

	private void setDslName() {
		if (dslFile == null) {
			return;
		}
		final String baseFileName = dslFile.getName();
		final int indexOfLastComma = baseFileName.lastIndexOf('.');
		if (indexOfLastComma < 0) {
			dslName = baseFileName;
		} else {
			dslName = baseFileName.substring(0, indexOfLastComma);
		}
		dslFileNamePrefix = dslName;
		final int indexOfHyphen = dslName.indexOf('-');
		if (indexOfHyphen >= 0) {
			dslName = dslName.substring(0, indexOfHyphen);
		}
	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<Object, Object> createDSLProperties()
			throws IOException {

		if (this.propertiesFile == null) {
			return new LinkedHashMap<Object, Object>();
		}

		try {
			GroovyClassLoader gcl = new GroovyClassLoader();
			Script script = (Script) gcl.parseClass(propertiesFile).newInstance();					
			ConfigObject config = new ConfigSlurper().parse(script);
			GroovySystem.getMetaClassRegistry().removeMetaClass(script.getClass());
			return config;
		} catch (final Exception e) {
			throw new IOException("Failed to read properties file: " + propertiesFile + ": " + e.getMessage(), e);
		}

	}

	private GroovyShell createGroovyShell(final LinkedHashMap<Object, Object> properties) {

		final String baseClassName = BaseDslScript.class.getName();

		final List<String> serviceJarFiles = createJarFileListForService();
		String classpathDir = null;
		if (this.getWorkDir() != null) {
			classpathDir = this.getWorkDir().getAbsolutePath();
		} else if (this.getDslFile() != null) {
			if (this.getDslFile().getParentFile() != null) {
				classpathDir = this.getDslFile().getParentFile().getAbsolutePath();
			}
		}

		if (classpathDir != null) {
			serviceJarFiles.add(classpathDir);
		}

		final CompilerConfiguration cc = createCompilerConfiguration(baseClassName, serviceJarFiles);

		final Binding binding = createGroovyBinding(properties);

		final GroovyShell gs = new GroovyShell(ServiceReader.class.getClassLoader(), binding, cc);

		return gs;
	}

	private static CompilerConfiguration createCompilerConfiguration(final String baseClassName,
			final List<String> extraJarFileNames) {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(STAR_IMPORTS);

		ic.addImports(CLASS_IMPORTS);

		ic.addStaticImport("Statistics",
				org.cloudifysource.domain.statistics.AbstractStatisticsDetails.class.getName(),
				"STATISTICS_FACTORY");
		cc.addCompilationCustomizers(ic);
		// cc.addCompilationCustomizers(ic, new ASTTransformationCustomizer(new DebugHookTransformar()));

		cc.setScriptBaseClass(baseClassName);

		cc.setClasspathList(extraJarFileNames);

		return cc;
	}

	private Binding createGroovyBinding(final LinkedHashMap<Object, Object> properties) {
		final Binding binding = new Binding();

		final Set<Entry<String, Object>> bindingPropertiesEntries = this.bindingProperties.entrySet();
		for (final Entry<String, Object> entry : bindingPropertiesEntries) {
			binding.setVariable(entry.getKey(), entry.getValue());
		}

		if (properties != null) {
			final Set<Entry<Object, Object>> entries = properties.entrySet();
			for (final Entry<Object, Object> entry : entries) {
				binding.setVariable((String) entry.getKey(), entry.getValue());
			}
			// add variable that contains all the properties
			// to distinguish between properties and other binding variables.
			// This will be used in loading application's service process
			// to transfer application properties to the service using the
			// application's binding.
			binding.setVariable(DSLUtils.DSL_PROPERTIES, properties);
			if (context != null) {
				binding.setVariable("context", context);
			}
		}

		binding.setVariable(DSLUtils.DSL_VALIDATE_OBJECTS_PROPERTY_NAME, validateObjects);
		binding.setVariable(DSLUtils.DSL_FILE_PATH_PROPERTY_NAME, dslFile == null ? null : dslFile.getPath());
		binding.setVariable(DSLReader.DSL_LOGGER_NAME, dslLogger);

		// MethodClosure printlnClosure = new MethodClosure(this, "println");
		// binding.setVariable("println", printlnClosure);

		return binding;
	}

	private List<String> createJarFileListForService() {

		logger.fine("Adding jar files to service compile path");
		if (!this.isLoadUsmLib()) {
			logger.fine("Ignoring usmlib - external jar files will not be added to classpath!");
			// when running in GSC, the usmlib jars are placed in the PU lib dir
			// automatically
			return new ArrayList<String>(0);
		}
		if (dslFile == null) {
			logger.fine("DSL file location not specified. Skipping usmlib jar loading!");
			return new ArrayList<String>(0);
		}

		final File serviceDir = dslFile.getParentFile();
		final File usmLibDir = new File(serviceDir, CloudifyConstants.USM_LIB_DIR);
		if (!usmLibDir.exists()) {
			logger.fine("No usmlib dir was found at: " + usmLibDir + " - no jars will be added to the classpath!");
			return new ArrayList<String>(0);
		}

		if (usmLibDir.isFile()) {
			throw new IllegalArgumentException("The service includes a file called: " + CloudifyConstants.USM_LIB_DIR
					+ ". This name may only be used for a directory containing service jar files");
		}

		final File[] libFiles = usmLibDir.listFiles();
		final List<String> result = new ArrayList<String>(libFiles.length);
		for (final File file : libFiles) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				result.add(file.getAbsolutePath());
			}
		}

		logger.fine("Extra jar files list: " + result);
		return result;
	}

	// //////////////
	// Accessors ///
	// //////////////

	public ServiceContext getContext() {
		return context;
	}

	public void setContext(final ServiceContext context) {
		this.context = context;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(final String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}

	public boolean isRunningInGSC() {
		return isRunningInGSC;
	}

	public void setRunningInGSC(final boolean isRunningInGSC) {
		this.isRunningInGSC = isRunningInGSC;
	}

	public File getDslFile() {
		return dslFile;
	}

	public void setDslFile(final File dslFile) {
		this.dslFile = dslFile;
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(final File workDir) {
		this.workDir = workDir;
	}

	public boolean isCreateServiceContext() {
		return createServiceContext;
	}

	public ClassLoader getDSLClassLoader() {
		return this.dslClassLoader;
	}

	public void setCreateServiceContext(final boolean createServiceContext) {
		this.createServiceContext = createServiceContext;
	}

	/**********
	 * .
	 * 
	 * @param key
	 *            .
	 * @param value
	 *            .
	 */
	public void addProperty(final String key, final Object value) {
		bindingProperties.put(key, value);

	}

	public void setDslContents(final String dslContents) {
		this.dslContents = dslContents;

	}

	public String getDslFileNameSuffix() {
		return dslFileNameSuffix;
	}

	public void setDslFileNameSuffix(final String dslFileNameSuffix) {
		this.dslFileNameSuffix = dslFileNameSuffix;
	}

	public void setOverridesScript(final String script) {
		this.overridesScript = script;
	}

	public String getDslName() {
		return dslName;
	}

	public void setDslName(final String dslName) {
		this.dslName = dslName;
	}

	public boolean isLoadUsmLib() {
		return loadUsmLib;
	}

	public void setLoadUsmLib(final boolean loadUsmLib) {
		this.loadUsmLib = loadUsmLib;
	}

	public void setBindingVariables(final Map<Object, Object> variables) {
		this.variables = variables;

	}

	public boolean isValidateObjects() {
		return validateObjects;
	}

	public void setValidateObjects(final boolean isValidateObjects) {
		this.validateObjects = isValidateObjects;
	}

	public File getPropertiesFile() {
		return this.propertiesFile;
	}

	public File getOverridesFile() {
		return this.overridesFile;
	}

	public void setOverridesFile(final File overridesFile) {
		this.overridesFile = overridesFile;
	}

	public Map<String, Object> getOverrides() {
		return this.overrideProperties;
	}

	public Map<String, Object> getOverrideFields() {
		return this.overrideFields;
	}

	public Map<String, Object> getApplicationProperties() {
		return applicationProperties;
	}

	public void setApplicationProperties(final Map<String, Object> applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

}
