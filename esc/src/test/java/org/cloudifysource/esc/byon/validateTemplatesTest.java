/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.byon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.CloudCompute;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class validateTemplatesTest {
	
	@Test
	public void testTempalteWithoutNodesList() throws IOException, AddTemplatesException, DSLException {
		// create template without nodes list
		ComputeTemplate template = new ComputeTemplate();
		
		// create the cloud object and add the template.
		Cloud cloud = new Cloud();
		CloudCompute cloudCompute = new CloudCompute();
		Map<String, ComputeTemplate> templates = new HashMap<String, ComputeTemplate>();
		templates.put("template", template);
		cloudCompute.setTemplates(templates);
		cloud.setCloudCompute(cloudCompute);
		
		// call updateDeployerTemplates, expecting CloudProvisioningException with the error message - "Nodes list not set"
		try {
			ByonProvisioningDriver driver = new ByonProvisioningDriver();
			ByonDeployer deployer = new ByonDeployer();
			driver.setDeployer(deployer);
			driver.updateDeployerTemplates(cloud);
		} catch (Exception e) {
			String message = e.getMessage();
			Assert.assertTrue("error message does not conatin Nodes list not set", message.contains("Nodes list not set"));
		} 
		
	}
}
