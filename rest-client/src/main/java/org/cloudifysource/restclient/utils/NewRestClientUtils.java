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
 ******************************************************************************/
package org.cloudifysource.restclient.utils;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;

/**
 * 
 * @author yael
 *
 */
public final class NewRestClientUtils {
    /**
     * Private C'tor, to avoid instantiation of this utility class.
     */
	private NewRestClientUtils() {
		
	}
	
	/**
	 * 
	 * @return true if the new rest client is enabled.
	 */
	public static boolean isNewRestClientEnabled() {
		String propertyValue = System.getProperty(CloudifyConstants.NEW_REST_CLIENT_ENABLE_PROPERTY);
		if (StringUtils.isBlank(propertyValue)) {
			return false;
		}
		return Boolean.parseBoolean(propertyValue);
	}
}
