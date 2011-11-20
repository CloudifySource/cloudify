package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.openspaces.admin.Admin;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.ui.UserInterface;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;

/*******
 * This class is a work in progress. DO NOT USE IT!
 * @author barakme
 *
 */

public class DSLReader {

	private GroovyShell gs;
	private boolean runningInGSC;
	private ClusterInfo clusterInfo;
	private Admin admin;
	private ServiceContext context;
	
	
	public void readDslEntity(final File dslFile, final File propertiesFile) throws DSLException {
		
		
		final LinkedHashMap<Object, Object> properties = createServiceProperties(propertiesFile);
		
		Object result = evaluateGroovyScript(dslFile, gs);

		if (result == null) {
			throw new IllegalStateException("The file " + dslFile
					+ " evaluates to null, not to a service object");
		}
		
		if (!(result instanceof Service)) {
			throw new IllegalStateException("The file: " + dslFile
					+ " did not evaluate to the required object type");
		}


	}

	
	private static GroovyShell initGroovyShell() {
		final CompilerConfiguration cc = createCompilerConfiguration();

		//final Binding binding = createGroovyBinding(properties, context);

		final GroovyShell gs = new GroovyShell(
				ServiceReader.class.getClassLoader(), // this.getClass().getClassLoader(),
				null , cc);
		
		return gs;
	}

	private static Binding createGroovyBinding(
			final LinkedHashMap<Object, Object> properties,
			ServiceContext context) {
		final Binding binding = new Binding();

		if (properties != null) {
			Set<Entry<Object, Object>> entries = properties.entrySet();
			for (Entry<Object, Object> entry : entries) {
				binding.setVariable((String) entry.getKey(), entry.getValue());
			}
		}
		
		if (context != null) {
			binding.setVariable("context", context);
		}
		return binding;
	}

	private static CompilerConfiguration createCompilerConfiguration() {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(com.gigaspaces.cloudify.dsl.Service.class
				.getPackage().getName(), UserInterface.class.getPackage()
				.getName(), com.gigaspaces.cloudify.dsl.context.Service.class
				.getPackage().getName());

		ic.addImports(com.gigaspaces.cloudify.dsl.utils.ServiceUtils.class
				.getName());

		cc.addCompilationCustomizers(ic);

		cc.setScriptBaseClass(BaseDslScript.class.getName());

		return cc;
	}


	private static Object evaluateGroovyScript(final File dslFile,
			final GroovyShell gs) {
		// Evaluate class using a FileReader, as the *-service files create a
		// class with an illegal name
		Object result = null;
		FileReader reader = null;
		try {
			reader = new FileReader(dslFile);
			result = gs.evaluate( reader, "service");
		} catch (final CompilationFailedException e) {
			throw new IllegalArgumentException("The file " + dslFile
					+ " could not be compiled", e);
		} catch (final IOException e) {
			throw new IllegalStateException("The file " + dslFile
					+ " could not be read", e);
		} catch (MissingMethodException e) {
			throw new IllegalArgumentException(
					"Could not resolve DSL entry with name: " + e.getMethod(),
					e);
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
			final File propertiesFile) throws DSLException {

		if(propertiesFile == null) {
			return new LinkedHashMap<Object, Object>();
		} else {
			try {
				ConfigObject config = new ConfigSlurper().parse(propertiesFile
						.toURI().toURL());

				return config;
			} catch (Exception e) {
				throw new DSLException("Failed to read properties file: "
						+ propertiesFile, e);
			}
		}
				
	}

}

