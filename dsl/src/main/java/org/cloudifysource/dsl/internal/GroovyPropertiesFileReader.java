package org.cloudifysource.dsl.internal;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import groovy.lang.Script;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class GroovyPropertiesFileReader {

	public ConfigObject readPropertiesFile(final File propertiesFile) throws IOException {
		if (propertiesFile == null) {
			return new ConfigObject();
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

	public ConfigObject readPropertiesScript(final String propertiesScript) throws IOException {
		if (propertiesScript == null || StringUtils.isBlank(propertiesScript)) {
			return new ConfigObject();
		}

		try {
			GroovyClassLoader gcl = new GroovyClassLoader();
			Script script = (Script) gcl.parseClass(propertiesScript).newInstance();					
			ConfigObject config = new ConfigSlurper().parse(script);
			GroovySystem.getMetaClassRegistry().removeMetaClass(script.getClass());
			return config;
		} catch (final Exception e) {
			throw new IOException("Failed to read properties script: " + propertiesScript+ ": " + e.getMessage(), e);
		}

	}

	@SuppressWarnings("unchecked")
	public Map<Object, Object> readPropertiesFileOld(final File file) throws IOException {
		if (file == null) {
			return new LinkedHashMap<Object, Object>();
		}

		ConfigObject config = new ConfigSlurper().parse(file.toURI().toURL());
		return config;
	}
}
