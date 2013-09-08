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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Manual test class for the driver.
 * 
 */
public class PrivateEC2CloudifyDriverIT {
	private static final Logger logger = Logger.getLogger(PrivateEC2CloudifyDriverIT.class.getName());

	private PrivateEC2CloudifyDriver driver;

	private Cloud cloud;

	@BeforeClass
	public static void beforeClass() {
		Logger logger = Logger.getLogger(PrivateEC2CloudifyDriverIT.class.getName());
		logger.setLevel(Level.FINEST);
		Handler[] handlers = logger.getParent().getHandlers();
		for (Handler handler : handlers) {
			handler.setLevel(Level.ALL);
		}
	}

	@Before
	public void before() throws CloudProvisioningException, DSLException {
		File file = new File("cloudify/clouds/privateEc2"); // Path to '*-cloud.groovy'
		logger.info(file.getAbsolutePath());
		cloud = ServiceReader.readCloudFromDirectory(file.getAbsolutePath(), null);
		driver = new PrivateEC2CloudifyDriver();
	}

	/**
	 * Requires a Management Machine up
	 */
	@Test
	public void testStartMachine() throws Exception {
		String cloudTemplateName = "CFN_TEMPLATE";

		ProvisioningContextImpl ctx = new ProvisioningContextImpl();
		ProvisioningContextAccess.setCurrentProvisioingContext(ctx);
		ctx.getInstallationDetailsBuilder().setCloud(this.cloud);
		ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(cloudTemplateName);
		ctx.getInstallationDetailsBuilder().setTemplate(template);

		driver.setCustomDataFile(new File("./cloudify/cfn-templates/sampleApplication"));
		driver.setConfig(cloud, cloudTemplateName, false, "someService");
		MachineDetails md = driver.startMachine(null, 60, TimeUnit.MINUTES);

		assertNotNull(md.getPrivateAddress());
		assertNotNull(md.getPublicAddress());
	}

	@Test
	public void testStopMachine() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/"));
		driver.setConfig(cloud, "CFN_TEMPLATE", true, "tags");
		driver.stopMachine("10.36.174.213", 60, TimeUnit.MINUTES);
	}

	@Test
	public void testStopMachineWithAmazonSDK() throws Exception {
		driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/"));
		driver.setConfig(cloud, cloud.getConfiguration().getManagementMachineTemplate(), true, "static-with-volume");
		driver.stopMachine("10.48.110.158", 1, TimeUnit.HOURS);
	}

	@Test
	public void testStartManagementMachines() throws Exception {
		ProvisioningContextImpl context = new ProvisioningContextImpl();
		ProvisioningContextAccess.setCurrentManagementProvisioingContext(context);
		context.getInstallationDetailsBuilder().setCloud(cloud);
		cloud.getConfiguration().getComponents().getDiscovery().setDiscoveryPort(4174);
		cloud.getCloudCompute().getTemplates().get("CFN_MANAGER_TEMPLATE").getCustom()
				.put("cfnManagerTemplate", "./cloudify/clouds/privateEc2/privateEc2-cfn.template");

		String cloudTemplateName = "CFN_MANAGER_TEMPLATE";
		driver.setConfig(cloud, cloudTemplateName, true, "someService");
		MachineDetails[] mds = driver.startManagementMachines(60, TimeUnit.MINUTES);
		for (MachineDetails md : mds) {
			assertNotNull(md.getPrivateAddress());
			assertNotNull(md.getPublicAddress());
		}
	}
}
