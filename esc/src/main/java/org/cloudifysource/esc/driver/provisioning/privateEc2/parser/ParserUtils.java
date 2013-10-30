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
package org.cloudifysource.esc.driver.provisioning.privateEc2.parser;

import java.io.File;
import java.io.InputStream;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Utility class to parse JSONF.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public final class ParserUtils {

	private ParserUtils() {

	}

	/**
	 * This method deserializes a json inputstream into the given object.
	 * 
	 * @param clazz
	 *            The class for deserialization.
	 * @param jsonStream
	 *            The json inputstream
	 * @param <T>
	 *            The converting type for json.
	 * @return The constructed JSON deserialization.
	 * 
	 * @throws PrivateEc2ParserException
	 *             If a problem occurs during the mapping.
	 */
	public static <T> T mapJson(final Class<T> clazz, final InputStream jsonStream) throws PrivateEc2ParserException {
		if (jsonStream == null) {
			return null;
		}

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.USE_ANNOTATIONS, true);
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		T tokenResponse = null;
		try {
			tokenResponse = mapper.readValue(jsonStream, clazz);
		} catch (Exception e) {
			throw new PrivateEc2ParserException(e);
		}
		return tokenResponse;
	}

	/**
	 * This method deserializes a json inputstream into the given object.
	 * 
	 * @param clazz
	 *            The class for deserialization.
	 * @param jsonString
	 *            The json in string format
	 * @param <T>
	 *            The converting type for json.
	 * @return The constructed JSON deserialization.
	 * 
	 * @throws PrivateEc2ParserException
	 *             If a problem occurs during the mapping.
	 */
	public static <T> T mapJson(final Class<T> clazz, final String jsonString) throws PrivateEc2ParserException {
		if (jsonString == null) {
			return null;
		}

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.USE_ANNOTATIONS, true);
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		T tokenResponse = null;
		try {
			tokenResponse = mapper.readValue(jsonString, clazz);
		} catch (Exception e) {
			throw new PrivateEc2ParserException(e);
		}
		return tokenResponse;
	}

	/**
	 * This method deserializes a json inputstream into the given object.
	 * 
	 * @param clazz
	 *            The class for deserialization.
	 * @param file
	 *            The json file
	 * @param <T>
	 *            The converting type for json.
	 * @return The constructed JSON deserialization.
	 * 
	 * @throws PrivateEc2ParserException
	 *             If a problem occurs during the mapping.
	 */
	public static <T> T mapJson(final Class<T> clazz, final File file) throws PrivateEc2ParserException {
		if (file == null) {
			return null;
		}
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.USE_ANNOTATIONS, true);
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		T tokenResponse = null;
		try {
			tokenResponse = mapper.readValue(file, clazz);
		} catch (Exception e) {
			throw new PrivateEc2ParserException(e);
		}
		return tokenResponse;
	}

}
