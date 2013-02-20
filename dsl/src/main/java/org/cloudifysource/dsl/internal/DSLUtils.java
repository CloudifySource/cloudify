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

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.internal.pu.InternalProcessingUnit;

/*********
 * Various utility functions used during DSL processing.
 * @author barakme
 *
 */
public final class DSLUtils {

	/**
	 * The context property set in application DSL files to indicate the directory where the application file itself can
	 * be found.
	 */
	public static final String APPLICATION_DIR = "workDirectory";
	/**
	 * The binding variable name of the DSL object properties.
	 */
	public static final String DSL_PROPERTIES = "dsl_properties";
	/**
	 * The binding variable name of the DSL file path - the path where the DSL file itself can be found.
	 */
	public static final String DSL_FILE_PATH_PROPERTY_NAME = "dslFilePath";
	/**
	 * The binding variable name of the validateObject flag - indicates if need to validate the DSL file.
	 */
	public static final String DSL_VALIDATE_OBJECTS_PROPERTY_NAME = "validateObjectsFlag";

	/************
	 * Default file name suffix for application files.
	 */
	public static final String APPLICATION_DSL_FILE_NAME_SUFFIX = DSLUtils.APPLICATION_FILE_NAME_SUFFIX + ".groovy";
	/**
	 * The expected file suffix for properties file.
	 */
	public static final String PROPERTIES_FILE_SUFFIX = ".properties";

	/**
	 * The expected file suffix for overrides file.
	 */
	public static final String OVERRIDES_FILE_SUFFIX = ".overrides";

	/**************
	 * Default file name suffix for cloud files.
	 */
	public static final String CLOUD_DSL_FILE_NAME_SUFFIX = "-cloud.groovy";
	/**
	 * The expected file name suffix for application files e.g. <code>*-application.groovy</code>.
	 */
	public static final String APPLICATION_FILE_NAME_SUFFIX = "-application";
	/*********
	 * Default file name suffix for service files.
	 */
	public static final String SERVICE_DSL_FILE_NAME_SUFFIX = "-service.groovy";

	/**
	 * Default file name suffix for template files.
	 */
	public static final String TEMPLATE_FILE_NAME_SUFFIX = "-template";
	/**
	 * The expected file name suffix for templates file e.g. <code>*-templates.groovy</code>.
	 */
	public static final String TEMPLATE_DSL_FILE_NAME_SUFFIX = "-template.groovy";
	/**
	 * The expected file name suffix for templates properties file e.g. <code>*-templates.properties</code>.
	 */
	public static final String TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX = "-template.properties";
	/**
	 * The expected file name suffix for templates overrides file e.g. <code>*-templates.overrides</code>.
	 */
	public static final String TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX = "-template.overrides";

	/**
	 * The expected file name of an application properties file after it has been copied to a service directory.
	 */
	public static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";

	/**
	 * The expected file name of an application overrides file after it has been copied to a service directory.
	 */
	public static final String APPLICATION_OVERRIDES_FILE_NAME = "application.overrides";

	/**
	 * The max number of templates allowed in one templates file.
	 */
	public static final int MAX_TEMPLATES_PER_FILE = 1;

	private DSLUtils() {
		// private constructor to prevent initialization
	}

	/**
	 *
	 * @param processingUnit
	 *            processingUnit
	 * @return the dependencies
	 */
	public static String getDependencies(final InternalProcessingUnit processingUnit) {
		final String dependencies = getContextPropertyValue(
				processingUnit, CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
		if (dependencies == null) {
			return "";
		}
		return dependencies;
	}

	/**
	 *
	 * @param processingUnit
	 *            processingUnit
	 * @return The tier type
	 */
	public static ServiceTierType getTierType(final InternalProcessingUnit processingUnit) {
		final String tierTypeStr = getContextPropertyValue(
				processingUnit, CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE);
		if (tierTypeStr == null) {
			return ServiceTierType.UNDEFINED;
		}
		return ServiceTierType.valueOf(tierTypeStr);
	}

	/**
	 *
	 * @param processingUnit
	 *            processingUnit
	 * @return The icon url
	 */
	public static String getIconUrl(final InternalProcessingUnit processingUnit) {
		final String iconUrlStr = getContextPropertyValue(
				processingUnit, CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON);
		if (iconUrlStr == null) {
			return "";
		}
		return iconUrlStr;
	}

	private static String getContextPropertyValue(final InternalProcessingUnit processingUnit,
			final String contextPropertyKey) {
		final String value = processingUnit.getBeanLevelProperties().getContextProperties().getProperty(
				contextPropertyKey);
		return value;
	}

	/**
	 * If dsl file name is tomcat-service.groovy than the properties file name expected to be tomcat-service.properties.
	 *
	 * @param dslDirectory
	 *            .
	 * @param fileNameSuffix
	 *            .
	 * @return The expected properties file name.
	 */
	public static String getPropertiesFileName(final File dslDirectory, final String fileNameSuffix) {
		File dslFile = DSLReader.
				findDefaultDSLFile(fileNameSuffix, dslDirectory);
		if (dslFile == null) {
			throw new IllegalArgumentException("DslUtils.getPropertiesFileName - a file with the given suffix ["
					+ fileNameSuffix + "] doesn't exist in " + dslDirectory + ".");
		}
		final String[] split = dslFile.getName().split("\\.");
		if (split.length == 0) {
			throw new IllegalArgumentException("DslUtils.getPropertiesFileName - file name ["
					+ dslFile.getName() + "] doesn't contain '.'");
		}
		return split[0] + PROPERTIES_FILE_SUFFIX;
	}

	/**
	 * Change the name of the file's prefix to template's name.
	 *
	 * @param file
	 *            the template's file.
	 * @param templateName
	 *            the template's name.
	 * @param suffix
	 *            the file suffix (-template.groovy/-template.properties/-template.overrides)
	 * @return returns the new name if the renaming needed and succeeded.
	 * @throws IOException
	 *             if failed to rename the file.
	 */
	public static String renameCloudTemplateFileNameIfNeeded(final File file, final String templateName,
			final String suffix)
			throws IOException {
		String fileName = file.getName();
		if (fileName.endsWith(suffix)) {
			String newName = templateName + suffix;
			if (!fileName.equals(newName)) {
				File parent = file.getParentFile();
				File newNameFile = new File(parent, newName);
				boolean renamed = file.renameTo(newNameFile);
				if (!renamed) {
					throw new IOException("Failed to rename file " + file.getAbsolutePath());
				}
				return newName;
			}
		}
		return null;
	}

	/**
	 * Checks if the given name contains chars that are invalid for Application or Service name.
	 *
	 * @param name
	 *            the Application or Service name to validate.
	 * @throws DSLValidationException
	 *             if the name if not valid.
	 */
	public static void validateRecipeName(final String name) throws DSLValidationException {
		char[] invalidChars = new char[] { '{', '}', '[', ']', '(', ')' };
		if (org.apache.commons.lang.StringUtils.containsAny(name, invalidChars)) {
			throw new DSLValidationException("The name \"" + name + "\" contains one or more invalid characters: "
					+ "'(',')','[',']','{','}'");
		}
	}

}
