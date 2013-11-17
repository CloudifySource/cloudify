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

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.JavaType;

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
	 * Serialize a json string into a javabean object. <br />
	 * Java property names are translate to lower case JSON element names.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param jsonString
	 *            The string to parse.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 * @throws OpenstackJsonSerializationException
	 *             If the json string could not be serialized into object.
	 */
	public static <T> T mapJsonToObject(final Class<T> clazz, final String jsonString)
			throws OpenstackJsonSerializationException {
		return mapJsonToObject(clazz, jsonString, true);
	}

	/**
	 * Serialize a json string into a javabean object.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param jsonString
	 *            The string to parse.
	 * @param translateCamelCase
	 *            Translates typical camel case Java property names to lower case JSON element names.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 * @throws OpenstackJsonSerializationException
	 *             If the json string could not be serialized into object.
	 */
	public static <T> T mapJsonToObject(final Class<T> clazz, final String jsonString, final boolean translateCamelCase)
			throws OpenstackJsonSerializationException {
		if (jsonString == null) {
			return null;
		}
		final ObjectMapper mapper = createDefaultDeserializationMapper();
		if (translateCamelCase) {
			mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
		}
		try {
			return mapper.readValue(jsonString, clazz);
		} catch (final Exception e) {
			throw new OpenstackJsonSerializationException(e);
		}
	}

	/**
	 * Serialize a json string into a javabean object and unwrap root-level JSON value.<br />
	 * Java property names are translate to lower case JSON element names.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param jsonString
	 *            The string to parse.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 * @throws OpenstackJsonSerializationException
	 *             If the json string could not be serialized into object.
	 */
	public static <T> T unwrapRootToObject(final Class<T> clazz, final String jsonString)
			throws OpenstackJsonSerializationException {
		return unwrapRootToObject(clazz, jsonString, true);
	}

	/**
	 * Serialize a json string into a javabean object and unwrap root-level JSON value.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param jsonString
	 *            The string to parse.
	 * @param translateCamelCase
	 *            Translates typical camel case Java property names to lower case JSON element names.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 * @throws OpenstackJsonSerializationException
	 *             If the json string could not be serialized into object.
	 */
	public static <T> T unwrapRootToObject(final Class<T> clazz, final String jsonString,
			final boolean translateCamelCase) throws OpenstackJsonSerializationException {
		if (jsonString == null) {
			return null;
		}
		try {
			final ObjectMapper mapper = createDefaultDeserializationMapper();
			mapper.configure(DeserializationConfig.Feature.UNWRAP_ROOT_VALUE, true);
			if (translateCamelCase) {
				mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
			}

			return mapper.readValue(jsonString, clazz);
		} catch (final Exception e) {
			throw new OpenstackJsonSerializationException(e);
		}
	}

	/**
	 * Serialize a json string into a javabean object and unwrap root-level JSON value.<br />
	 * Java property names are translate to lower case JSON element names.
	 * 
	 * @param clazz
	 *            The target class.
	 * @param jsonString
	 *            The string to parse.
	 * @param <T>
	 *            The type of the java bean.
	 * @return The class filled with the json values. Fields that are not defined in the java bean will be ignore.
	 * @throws OpenstackJsonSerializationException
	 *             If the json string could not be serialized into object.
	 */
	public static <T> List<T> unwrapRootToList(final Class<T> clazz, final String jsonString)
			throws OpenstackJsonSerializationException {
		List<T> list = null;
		try {
			final ObjectMapper mapper = createDefaultDeserializationMapper();
			mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

			final JsonNode readTree = mapper.readTree(jsonString);
			if (readTree != null && readTree.getElements().hasNext()) {
				final JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
				JsonNode next = readTree.getElements().next();
				if (!"[0]".equals(next.toString())) {
					list = mapper.readValue(next, type);
				}
			}

		} catch (final Exception e) {
			throw new OpenstackJsonSerializationException(e);
		}
		return list;
	}

	private static ObjectMapper createDefaultDeserializationMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true);
		mapper.configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, true);
		mapper.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, false);
		return mapper;
	}

	/**
	 * Deserialize a javabean object into json string.<br />
	 * Java property names are translate to lower case JSON element names.
	 * 
	 * @param javabean
	 *            The object to deserialize.
	 * @return The json representation of the javabean.
	 * @throws OpenstackJsonSerializationException
	 *             If the javabean could not be deserialized.
	 */
	public static String toJson(final Object javabean) throws OpenstackJsonSerializationException {
		return toJson(javabean, true);
	}

	/**
	 * Deserialize a javabean object into json string.
	 * 
	 * @param javabean
	 *            The object to deserialize.
	 * @param translateCamelCase
	 *            Translates typical camel case Java property names to lower case JSON element names.
	 * @return The json representation of the javabean.
	 * @throws OpenstackJsonSerializationException
	 *             If the javabean could not be deserialized.
	 */
	public static String toJson(final Object javabean, final boolean translateCamelCase)
			throws OpenstackJsonSerializationException {
		if (javabean == null) {
			return null;
		}
		try {
			final ObjectMapper mapper = new ObjectMapper();

			if (translateCamelCase) {
				mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
			}
			mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
			mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
			mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
			mapper.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, true);
			mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
					.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
					.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
					.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
					.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

			final String json = mapper.writeValueAsString(javabean);
			return json;
		} catch (final Exception e) {
			throw new OpenstackJsonSerializationException(e);
		}
	}
}
