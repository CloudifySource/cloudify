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
package org.cloudifysource.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.internal.context.ServiceContextImpl;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.openspaces.admin.Admin;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.ui.UserInterface;

/*******
 * This class is a work in progress. DO NOT USE IT!
 * 
 * @author barakme
 * 
 */

public class DSLReader {

	private static Logger logger = Logger.getLogger(DSLReader.class.getName());

	private boolean loadUsmLib = true;

	private ClusterInfo clusterInfo;
	private Admin admin;
	private ServiceContextImpl context;

	private String propertiesFileName;

	private boolean isRunningInGSC;

	private File dslFile;
	private File workDir;

	private String dslFileNameSuffix;

	private File propertiesFile;

	private boolean createServiceContext = true;

	private final Map<String, Object> bindingProperties = new HashMap<String, Object>();

	private String dslContents;

	/*********
	 * Default file name suffix for service files.
	 */
	public static final String SERVICE_DSL_FILE_NAME_SUFFIX = "-service.groovy";
	/************
	 * Default file name suffix for application files.
	 */
	public static final String APPLICATION_DSL_FILE_NAME_SUFFIX = "-application.groovy";
	/**************
	 * Default file name suffix for cloud files.
	 */
	public static final String CLOUD_DSL_FILE_NAME_SUFFIX = "-cloud.groovy";

	private static final String[] STAR_IMPORTS = new String[] {
			org.cloudifysource.dsl.Service.class.getPackage().getName(), UserInterface.class.getPackage().getName(),
			org.cloudifysource.dsl.internal.context.ServiceImpl.class.getPackage().getName() };

	private static final String DSL_FILE_PATH_PROPERTY_NAME = "dslFilePath";

	private void initDslFile()
			throws FileNotFoundException {
		if (dslFile != null) {
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

		this.dslFile = findDefaultDSLFile(dslFileNameSuffix, workDir);

	}

	/***********
	 * .
	 * 
	 * @param fileNameSuffix .
	 * @param dir .
	 * @return .
	 */
	public static File findDefaultDSLFile(final String fileNameSuffix, final File dir) {

		final File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(fileNameSuffix);
			}
		});

		if (files.length > 1) {
			throw new IllegalArgumentException("Found multiple configuration files: " + Arrays.toString(files) + ". "
					+ "Only one may be supplied in the folder.");
		}

		if (files.length == 0) {
			throw new IllegalArgumentException("Cannot find configuration file in " + dir.getAbsolutePath());
		}

		return files[0];
	}

	private void init()
			throws IOException {
		initDslFile();
		initPropertiesFile();

	}

	/*********
	 * Executes the current DSL reader, returning the required Object type.
	 * 
	 * @param clazz the expected class type returned from the DSL file.
	 * @param <T> The Class type returned from this type of DSL file.
	 * @return the domain POJO.
	 * @throws DSLException in case there was a problem processing the DSL file.
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
		} catch (final Exception e) {
			// catching exception here, as groovy config slurper may throw just
			// about anything
			throw new IllegalArgumentException("Failed to load properties file " + this.propertiesFile, e);
		}

		ClusterInfo clusterInfoToUseInGsc = this.clusterInfo;
		if (clusterInfoToUseInGsc == null) {
			clusterInfoToUseInGsc = new ClusterInfo(null, 1, 0, 1, 0);
		}

		// create an uninitialized service context
		if (this.createServiceContext) {
			if (isRunningInGSC) {
				this.context = new ServiceContextImpl(clusterInfoToUseInGsc, workDir.getAbsolutePath());
			} else {
				this.context = new ServiceContextImpl(new ClusterInfo(null, 1, 0, 1, 0), workDir.getAbsolutePath());
			}

		}
		// create the groovy shell, loaded with our settings
		final GroovyShell gs = createGroovyShell(properties);
		final Object result = evaluateGroovyScript(gs);

		if (this.createServiceContext) {
			if (!(result instanceof Service)) {
				throw new IllegalArgumentException(
						"The DSL reader cannot create a service context to a DSL that does not evaluate to a Sevice. "
								+ "Set the 'createServiceContext' option to false if you do not need a service conext");
			}

			if (isRunningInGSC) {
				if (clusterInfoToUseInGsc.getName() == null) {
					clusterInfoToUseInGsc.setName(ServiceUtils.getAbsolutePUName(
							CloudifyConstants.DEFAULT_APPLICATION_NAME, ((Service) result).getName()));
				}
				this.context.init((Service) result, admin, clusterInfoToUseInGsc);
			} else {
				this.context.initInIntegratedContainer((Service) result);
			}
		}

		return result;

	}

	private Object evaluateGroovyScript(final GroovyShell gs)
			throws DSLValidationException {
		// Evaluate class using a FileReader, as the *-service files create a
		// class with an illegal name
		Object result = null;

		if (this.dslContents == null) {

			FileReader reader = null;
			try {
				reader = new FileReader(dslFile);
				result = gs.evaluate(reader, "dslEntity");
			} catch (final IOException e) {
				throw new IllegalStateException("The file " + dslFile + " could not be read", e);
			} catch (final MissingMethodException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getMethod(), e);
			} catch (final MissingPropertyException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getProperty(), e);
			} catch (final DSLValidationRuntimeException e) {
				throw e.getDSLValidationException();
			} finally {
				if (reader != null) {
					try {
						reader.close();
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
		final String baseFileName = dslFile.getName();

		final int indexOfLastComma = baseFileName.lastIndexOf('.');
		String fileNamePrefix;
		if (indexOfLastComma < 0) {
			fileNamePrefix = baseFileName;
		} else {
			fileNamePrefix = baseFileName.substring(0, indexOfLastComma);
		}

		final String defaultPropertiesFileName = fileNamePrefix + ".properties";

		final File defaultPropertiesFile = new File(workDir, defaultPropertiesFileName);

		if (defaultPropertiesFile.exists()) {
			this.propertiesFileName = defaultPropertiesFileName;
			this.propertiesFile = defaultPropertiesFile;
		}

	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<Object, Object> createDSLProperties()
			throws IOException {

		if (this.propertiesFile == null) {
			return new LinkedHashMap<Object, Object>();
		}

		try {
			final ConfigObject config = new ConfigSlurper().parse(propertiesFile.toURI().toURL());

			return config;
		} catch (final Exception e) {
			throw new IOException("Failed to read properties file: " + propertiesFile, e);
		}

	}

	private GroovyShell createGroovyShell(final LinkedHashMap<Object, Object> properties) {

		final String baseClassName = BaseDslScript.class.getName();

		final List<String> serviceJarFiles = createJarFileListForService();
		final CompilerConfiguration cc = createCompilerConfiguration(baseClassName, serviceJarFiles);

		final Binding binding = createGroovyBinding(properties, context, dslFile);

		final GroovyShell gs = new GroovyShell(ServiceReader.class.getClassLoader(), binding, cc);
		return gs;
	}

	private CompilerConfiguration createCompilerConfiguration(final String baseClassName,
			final List<String> extraJarFileNames) {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(STAR_IMPORTS);

		ic.addImports(org.cloudifysource.dsl.utils.ServiceUtils.class.getName());

		ic.addStaticImport("Statistics", org.cloudifysource.dsl.statistics.AbstractStatisticsDetails.class.getName(),
				"STATISTICS_FACTORY");
		cc.addCompilationCustomizers(ic);

		cc.setScriptBaseClass(baseClassName);

		cc.setClasspathList(extraJarFileNames);
		return cc;
	}

	private Binding createGroovyBinding(final LinkedHashMap<Object, Object> properties, final ServiceContext context,
			final File dslFile) {
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
			if (context != null) {
				binding.setVariable("context", context);
			}
		}

		binding.setVariable(DSL_FILE_PATH_PROPERTY_NAME, dslFile == null ? null : dslFile.getPath());
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

	public ClusterInfo getClusterInfo() {
		return clusterInfo;
	}

	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public ServiceContextImpl getContext() {
		return context;
	}

	public void setContext(final ServiceContextImpl context) {
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

	public void setCreateServiceContext(final boolean createServiceContext) {
		this.createServiceContext = createServiceContext;
	}

	/**********
	 * .
	 * 
	 * @param key .
	 * @param value .
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

	public boolean isLoadUsmLib() {
		return loadUsmLib;
	}

	public void setLoadUsmLib(final boolean loadUsmLib) {
		this.loadUsmLib = loadUsmLib;
	}

}
