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
	@Deprecated
	public static <T> T mapJsonToObject(final Class<T> clazz, final String response) {
		if (response == null) {
			return null;
		}
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		T tokenResponse = null;
		try {
			tokenResponse = mapper.readValue(response, clazz);
		} catch (final Exception e) {
			e.printStackTrace();
			// FIXME
			// throw new OpenstackJsonException(e);
		}
		return tokenResponse;
	}

	public static <T> T unwrapRootToObject(final Class<T> clazz, final String response)
			throws OpenstackJsonSerializationException {
		return unwrapRootToObject(clazz, response, false);
	}

	public static <T> T unwrapRootToObject(final Class<T> clazz, final String response, final boolean useCamelCase)
			throws OpenstackJsonSerializationException {
		if (response == null) {
			return null;
		}
		T tokenResponse = null;
		try {
			final ObjectMapper mapper = new ObjectMapper();
			if (!useCamelCase) {
				mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
			}
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true);
			mapper.configure(DeserializationConfig.Feature.UNWRAP_ROOT_VALUE, true);
			mapper.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, false);
			tokenResponse = mapper.readValue(response, clazz);
		} catch (final Exception e) {
			throw new OpenstackJsonSerializationException(e);
		}
		return tokenResponse;
	}

	public static <T> List<T> unwrapRootToList(final Class<T> clazz, final String response)
			throws OpenstackJsonSerializationException {
		List<T> list = null;
		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true);
			mapper.configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, true);

			final JsonNode readTree = mapper.readTree(response);
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

	public static String toJson(final Object javabean) throws OpenstackJsonSerializationException {
		return toJson(javabean, false);
	}

	public static String toJson(final Object javabean, final boolean useCamelCase)
			throws OpenstackJsonSerializationException {
		if (javabean == null) {
			return null;
		}
		try {
			final ObjectMapper mapper = new ObjectMapper();

			if (!useCamelCase) {
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
