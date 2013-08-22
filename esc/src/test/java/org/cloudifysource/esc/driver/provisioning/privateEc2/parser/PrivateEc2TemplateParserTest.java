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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.junit.Ignore;
import org.junit.Test;

public class PrivateEc2TemplateParserTest {

	@Test
	public void test() throws Exception {
		String templateFile = "./cfn_templates/WordPress_Single_Instance_With_RDS.template";
		InputStream templateStream = ClassLoader.getSystemResourceAsStream(templateFile);
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
		assertNotNull(template.getEC2Instance());
		assertNotNull(template.getEC2Instance().getProperties());
		assertNotNull(template.getEC2Instance().getProperties().getImageId().getValue());
		assertNull(template.getEC2Volume(null));
	}

	/**
	 * @throws IOException
	 * @throws PrivateEc2ParserException
	 * 
	 */
	@Test
	@Ignore
	public void testTemplateWithEBS() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/EC2WithEBSSample.template");
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
		assertNotNull(template.getEC2Instance());
		assertNotNull(template.getEC2Instance().getProperties());
		assertNotNull(template.getEC2Instance().getProperties().getVolumes());
		assertEquals(1, template.getEC2Instance().getProperties().getVolumes().size());
		assertNotNull(template.getEC2Instance().getProperties().getVolumes().get(0).getVolumeId());
		assertNotNull("NewVolume", template.getEC2Instance().getProperties().getVolumes().get(0).getVolumeId()
				.getValue());
		assertNotNull(template.getEC2Volume("NewVolume"));
	}

	@Test
	public void testVolumeTemplate() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/volume.template");
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
		assertNotNull(template);
		assertNotNull(template.getEC2Instance());
		assertNotNull(template.getEC2Instance().getProperties());
		assertNotNull(template.getEC2Instance().getProperties().getVolumes());
		assertEquals(1, template.getEC2Instance().getProperties().getVolumes().size());
		assertNotNull(template.getEC2Instance().getProperties().getVolumes().get(0).getVolumeId());
		assertNotNull("smallVolume", template.getEC2Instance().getProperties().getVolumes().get(0).getVolumeId()
				.getValue());
		assertNotNull(template.getEC2Volume("smallVolume"));

	}

	@Test
	public void testJoinTemplate() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/join.template");
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
		assertNotNull(template);
		assertNotNull(template.getEC2Instance());
		assertNotNull(template.getEC2Instance().getProperties());
		assertNotNull(template.getEC2Instance().getProperties().getAvailabilityZone());
		assertNotNull(template.getEC2Instance().getProperties().getUserData());
		assertEquals("export NIC_ADDR=`hostname`\nexport JAVA_HOME=/home/ubuntu/java\n", template.getEC2Instance()
				.getProperties().getUserData().getValue());
	}

	@Test
	public void testTemplateComplete() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/complete.template");
		ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
	}

	@Test
	public void testTemplateWithRef() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/ref.template");
		ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
	}

	@Test
	public void testTemplateWithTags() throws IOException, PrivateEc2ParserException {
		InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/tags.template");
		PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
		assertNotNull(template);
		assertNotNull(template.getEC2Instance().getProperties().getTags());
		assertFalse(template.getEC2Instance().getProperties().getTags().isEmpty());
	}
}
