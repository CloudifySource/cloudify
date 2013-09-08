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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.ParserUtils;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.PrivateEc2ParserException;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Instance;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Volume;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.Base64Function;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.RefValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ValueDeserializerTest {

	@Test
	public void testDeserializeString() throws PrivateEc2ParserException {
		SimpleValue sample = ParserUtils.mapJson(SimpleValue.class, "{\"Value\": \"hello world\"}");
		assertNotNull("Value is null", sample.getValueType());
		Assert.assertThat(sample.getValueType().getValue(), CoreMatchers.is("hello world"));
	}

	@Test
	public void testDeserializeBase64() throws PrivateEc2ParserException {
		SimpleValue mapJson =
				ParserUtils.mapJson(SimpleValue.class, "{\"Value\": { \"Fn::Base64\" : \"hello world\" }}");
		assertNotNull(mapJson.getValueType());
		String helloWorldBase64 = StringUtils.newStringUtf8(Base64.encodeBase64("hello world".getBytes()));
		Assert.assertThat(mapJson.getValueType().getValue(), CoreMatchers.is("hello world"));
		Assert.assertThat(((Base64Function) mapJson.getValueType()).getEncodedValue(),
				CoreMatchers.is(helloWorldBase64));
	}

	@Test
	public void testDeserializeJoin() throws PrivateEc2ParserException {
		String template = "{\"Value\": { \"Fn::Join\" : [\" \", [\"hello\",\"pretty\",\"world\"]]}}";
		SimpleValue mapJson = ParserUtils.mapJson(SimpleValue.class, template);
		assertNotNull(mapJson.getValueType());
		assertThat(mapJson.getValueType().getValue(), is("hello pretty world "));
	}

	@Test
	public void testDeserializeBase64AndJoin() throws Exception {
		String template =
				"{\"Value\": { \"Fn::Base64\" : { \"Fn::Join\" : [\" \", [\"hello\",\"pretty\",\"world\"]]}}}";
		SimpleValue mapJson = ParserUtils.mapJson(SimpleValue.class, template);
		assertNotNull(mapJson.getValueType());
		String base64 = StringUtils.newStringUtf8(Base64.encodeBase64("hello pretty world ".getBytes()));
		Assert.assertThat(mapJson.getValueType().getValue(), CoreMatchers.is("hello pretty world "));
		assertThat(((Base64Function) mapJson.getValueType()).getEncodedValue(), is(base64));
	}

	@Test
	public void testDeserializeRef() throws PrivateEc2ParserException {
		PrivateEc2Template template =
				ParserUtils.mapJson(PrivateEc2Template.class, new File(
						"./src/test/resources/cfn_templates/ref.template"));
		assertNotNull(template);

		AWSEC2Instance ec2instance = template.getEC2Instance();
		AWSEC2Volume volume = template.getEC2Volume("smallVolume");

		assertNotNull(ec2instance);
		assertNotNull(volume);
		ValueType volumeId = ec2instance.getProperties().getVolumes().get(0).getVolumeId();
		assertThat(volumeId, instanceOf(RefValue.class));
		assertThat(volumeId.getValue(), equalTo(volume.getResourceName()));

	}
}
