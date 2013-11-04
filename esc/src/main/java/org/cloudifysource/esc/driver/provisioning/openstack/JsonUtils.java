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
package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A utility class to map json to javabeans.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public final class JsonUtils {

	private JsonUtils() {
	}

	/**
	 * Map a json string to a javabean.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param response
	 *            The string to parse.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 */
	public static <T> T mapJsonToObject(final Class<T> clazz, final String response) {
		if (response == null) {
			return null;
		}
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		T tokenResponse = null;
		try {
			tokenResponse = mapper.readValue(response, clazz);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tokenResponse;

	}
}
