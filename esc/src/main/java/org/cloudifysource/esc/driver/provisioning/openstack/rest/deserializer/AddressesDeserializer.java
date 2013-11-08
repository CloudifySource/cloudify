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
package org.cloudifysource.esc.driver.provisioning.openstack.rest.deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerAddress;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author victor
 * @since 2.7.0
 */
public class AddressesDeserializer extends JsonDeserializer<List<NovaServerAddress>> {

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	public AddressesDeserializer() {
	}

	@Override
	public List<NovaServerAddress> deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		if (JsonToken.START_OBJECT != jp.getCurrentToken()) {
			throw ctxt.mappingException("Expected START OBJECT got: " + jp.getCurrentName());
		}

		final List<NovaServerAddress> addresses = new ArrayList<NovaServerAddress>();
		while (JsonToken.END_OBJECT != jp.nextToken()) {
			final List<NovaServerAddress> deserialized = this.deserializeResource(jp, ctxt);
			if (deserialized != null) {
				addresses.addAll(deserialized);
			}
		}
		return addresses;
	}

	/**
	 * Deserialize a server address resource.
	 * 
	 * The addresses node looks like this:
	 * 
	 * <pre>
	 * {
	 *    "addresses":{
	 *       "network1":[
	 *          {
	 *             "version":4,
	 *             "addr":"186.2.0.4",
	 *             "OS-EXT-IPS:type":"fixed"
	 *          },
	 *          {
	 *             "version":4,
	 *             "addr":"186.2.0.3",
	 *             "OS-EXT-IPS:type":"fixed"
	 *          }
	 *       ],
	 *       "network2":[
	 *          {
	 *             "version":4,
	 *             "addr":"172.16.0.14",
	 *             "OS-EXT-IPS:type":"fixed"
	 *          }
	 *       ]
	 *    }
	 * }
	 * </pre>
	 * 
	 */
	private List<NovaServerAddress> deserializeResource(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		final String addressName = jp.getText();
		jp.nextToken(); // skip current address name

		final List<NovaServerAddress> list = new ArrayList<NovaServerAddress>();
		final ObjectMapper mapr = new ObjectMapper();
		mapr.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		while (JsonToken.END_ARRAY != jp.nextToken()) {
			final NovaServerAddress address = mapr.readValue(jp, NovaServerAddress.class);
			address.setName(addressName);
			list.add(address);
		}

		return list;
	}
}
