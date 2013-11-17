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
package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.Base64Function;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.JoinFunction;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.RefValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.StringValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * A deserializer which handle property values of Amazon CloudFormation.<br />
 * It create a specific bean for :
 * <ul>
 * <li><code>Fn::Base64</code></li>
 * <li><code>Fn::Join</code></li>
 * <li><code>Ref</code></li>
 * <li><code>basic string values</code></li>
 * </ul>
 * The other type of value is simply saved as a raw in a {@link StringValue}.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class ValueDeserializer extends JsonDeserializer<ValueType> {
	private static final Logger logger = Logger.getLogger(ValueDeserializer.class.getName());

	@Override
	public ValueType deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = jp.getCodec();
		JsonNode node = oc.readTree(jp);
		return this.functionValue(node, ctxt);
	}

	private ValueType functionValue(final JsonNode root, final DeserializationContext ctxt) throws IOException {

		Iterator<String> fieldNames = root.getFieldNames();

		while (fieldNames.hasNext()) {
			String next = fieldNames.next();

			if ("Fn::Base64".equals(next)) {
				JsonNode jsonNode = root.get(next);
				ValueType value = this.functionValue(jsonNode, ctxt);
				return new Base64Function(value);
			} else if ("Fn::Join".equals(next)) {
				JsonNode joinNode = root.get(next);
				Iterator<JsonNode> elements = joinNode.getElements();

				JsonNode separatorNode = elements.next();
				String separator = separatorNode.getTextValue();

				JsonNode toJoinNodes = elements.next();
				Iterator<JsonNode> iterator = toJoinNodes.iterator();
				List<ValueType> toJoinList = new ArrayList<ValueType>();
				while (iterator.hasNext()) {
					JsonNode node = iterator.next();
					toJoinList.add(this.functionValue(node, ctxt));
				}

				return new JoinFunction(separator, toJoinList);
			} else if ("Ref".equals(next)) {
				return new RefValue(root.get(next).getValueAsText());
			} else {
				logger.warning("Value not supported: " + next + " - node: " + root.toString());
				return new StringValue(root.toString());
			}
		}
		return new StringValue(root.getTextValue());
	}
}
