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
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Instance;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Volume;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.VolumeMapping;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PrivateEC2CloudifyDriverTest {
	private static final Logger logger = Logger.getLogger(PrivateEC2CloudifyDriverTest.class.getName());

	private PrivateEC2CloudifyDriver driver;

	@Before
	public void before() {
		this.driver = new PrivateEC2CloudifyDriver();
	}

	@Test
	public void testGetDriverContextStatic() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/static-cfn.template"));
		PrivateEc2Template template = (PrivateEc2Template) driver.getCFNTemplatePerService("static");
		Assert.assertNotNull(template);
		logger.info(template.toString());
		Assert.assertNotNull(template.getResources());
		Assert.assertNotNull(template.getEC2Instance().getProperties().getImageId());
	}

	@Test
	public void testGetDriverContextStaticWithVolume() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/static-with-volume-cfn.template"));
		PrivateEc2Template template = (PrivateEc2Template) driver.getCFNTemplatePerService("static-with-volume");
		Assert.assertNotNull(template);
		logger.info(template.toString());
		Assert.assertNotNull(template.getResources());
		AWSEC2Instance awsec2Instance = (AWSEC2Instance) template.getEC2Instance();
		Assert.assertNotNull(awsec2Instance.getProperties().getImageId());
		Assert.assertNotNull(awsec2Instance.getProperties().getVolumes());
		Assert.assertNotNull(((AWSEC2Volume) template.getEC2Volume("smallVolume")).getProperties().getSize());
	}

	@Test
	public void testGetDriverContextFolder() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates"));

		String templateName = "static-with-volume";

		PrivateEc2Template template = (PrivateEc2Template) driver.getCFNTemplatePerService(templateName);
		Assert.assertNotNull(template);
		logger.info(template.toString());
		Assert.assertNotNull(template.getResources());
		AWSEC2Instance awsec2Instance = template.getEC2Instance();
		Assert.assertNotNull(awsec2Instance.getProperties().getImageId());
		List<VolumeMapping> volumes = awsec2Instance.getProperties().getVolumes();
		Assert.assertNotNull(volumes);
		Assert.assertFalse(volumes.isEmpty());
		Assert.assertNotNull(volumes.get(0).getVolumeId());
		String volumeName = volumes.get(0).getVolumeId().getValue();
		Assert.assertNotNull((template.getEC2Volume(volumeName)).getProperties().getSize());
	}

	@Test
	public void testManagerCFN() throws Exception {
		String managerCfnTemplateFile = "privateEc2-cfn.template";
		final File cloudDirectory = new File("src/main/resources/clouds/privateEc2/");
		Assert.assertTrue(cloudDirectory.exists() && cloudDirectory.isDirectory());
		PrivateEc2Template template = driver.getPrivateEc2TemplateFromFile(cloudDirectory, managerCfnTemplateFile);
		Assert.assertNotNull(template);
		logger.info(template.toString());
		Assert.assertNotNull(template.getResources());
		AWSEC2Instance awsec2Instance = (AWSEC2Instance) template.getEC2Instance();
		Assert.assertNotNull(awsec2Instance.getProperties().getImageId());
		List<VolumeMapping> volumes = awsec2Instance.getProperties().getVolumes();
		Assert.assertNull(volumes);
	}

	@Test
	public void testProperties() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates"));

		PrivateEc2Template template = driver.getCFNTemplatePerService("externalConfig");
		ValueType imageId = template.getEC2Instance().getProperties().getImageId();
		assertThat(imageId.getValue(), is("ami-23d9a94a"));

		ValueType keyName = template.getEC2Instance().getProperties().getKeyName();
		assertThat(keyName.getValue(), is("cloudify"));
	}

}
