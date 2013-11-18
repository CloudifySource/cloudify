/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.util;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FilenameFilter;

import org.codehaus.groovy.control.CompilerConfiguration;

/**
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public class ProvisioningDriverClassBuilder {

	/**
	 * Load ProvisioningClass class from classpath and instantiate it. 
	 * 
	 * @param className 
	 * 			the class name to instantiate.
	 * @return 
	 * 			a new ProvisioningDriver instance
	 * @throws ClassNotFoundException .
	 * @throws InstantiationException . 
	 * @throws IllegalAccessException .
	 */
	public Object build(final String className) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return Class.forName(className).newInstance();
	}

	/**
	 * loads external packages from <cloudFolder/lib>  into classpath and returns a cloud driver instance.
	 * 
	 * @param cloudFolder
	 * 		The cloud folder.
	 * @param className
	 * 		The required class name.
	 * @return
	 * 		a ProvisioningDriver instance.
	 * @throws ClassNotFoundException .
	 * @throws InstantiationException .
	 * @throws IllegalAccessException .
	 */
	public Object build(final String cloudFolder, final String className) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final File cloudLibFolder = new File(cloudFolder, "lib");
		if (cloudLibFolder.exists()) {
			String libFolderPath = cloudLibFolder.getAbsolutePath();
			final CompilerConfiguration gcc = new CompilerConfiguration();
			//add lib folder to the groovy classpath.
			gcc.getClasspath().add(libFolderPath);
			final File[] jarFiles = listJarFiles(cloudLibFolder);
			for (File file : jarFiles) {
				// add all jars in folder to the classpath.
				gcc.getClasspath().add(file.getAbsolutePath());
			}
			//create new groovy classloader having current class loader as parent.
			final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			final GroovyClassLoader gcl = new GroovyClassLoader(ccl, gcc);
			return gcl.loadClass(className).newInstance();
		}
		return build(className);

	}

	private File[] listJarFiles(final File libFolder) {
		return libFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(".jar");
			}
		});
	}
}
