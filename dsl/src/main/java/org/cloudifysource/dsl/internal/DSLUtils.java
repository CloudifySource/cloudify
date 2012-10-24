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

import org.openspaces.admin.internal.pu.InternalProcessingUnit;


public final class DSLUtils {


	/**
	 * The context property set in application DSL files to indicate 
	 * the directory where the application file itself can be found.
	 */
	public static final String APPLICATION_DIR = "workDirectory";
	
	/**
	 * The context property name of the DSL object properties as parsed at {@link DSLReader}.
	 */
	public static final String DSL_PROPERTIES = "dsl_properties";

	public static final String OVERRIDES_FILE_SUFFIX = ".overrides";

	public static final String APPLICATION_FILE_NAME_SUFFIX = "-application";

	
	private DSLUtils() {
		// private constructor to prevent initialization
	}

	/**
	 * 
	 * @param processingUnit processingUnit
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
	 * @param processingUnit processingUnit
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
	 * @param processingUnit processingUnit
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

}
