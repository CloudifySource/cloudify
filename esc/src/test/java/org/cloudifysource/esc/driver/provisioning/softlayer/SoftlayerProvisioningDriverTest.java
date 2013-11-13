/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.esc.driver.provisioning.softlayer;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.jclouds.softlayer.SoftlayerProvisioningDriver;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

/**
 * Test functionality of {@link org.cloudifysource.esc.driver.provisioning.jclouds.softlayer.SoftlayerProvisioningDriver}
 *
 * @author Eli Polonsky
 * @since 2.7.0
 */
public class SoftlayerProvisioningDriverTest {

    /**
     * Test that the correct module was setup for softlayer.
     * This module is a one that does not return location, image, and hardware in node meta data.
     *
     * Since this is a live test, we don't run it in travis.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testSetupModules() throws Exception {

        SoftlayerProvisioningDriver driver = new SoftlayerProvisioningDriver();

        Cloud cloud = createCloud();

        driver.setCloudTemplateName("dummy");
        driver.initDeployer(cloud);

        ComputeService compute = ((ComputeServiceContext) driver.getComputeContext()).getComputeService();

        Set<? extends ComputeMetadata> computeMetadatas = compute.listNodes();

        if (computeMetadatas.isEmpty()) {
            throw new IllegalStateException("There are no nodes in the account");
        }

        for (ComputeMetadata computeMetadata : computeMetadatas) {
            NodeMetadata node = (NodeMetadata) computeMetadata;
            Assert.assertNull(node.getLocation());
            Assert.assertNull(node.getHardware());
            Assert.assertNull(node.getImageId());

        }


    }

    private Cloud createCloud() {

        Cloud cloud = new Cloud();
        String user = System.getProperty("org.cloudifysource.test.softlayer.user");
        String apiKey = System.getProperty("org.cloudifysource.test.softlayer.api-key");

        if (StringUtils.isBlank(user)) {
            throw new IllegalStateException("user is null. please set 'org.cloudifysource.test.softlayer.user' system"
                    + " property and run again.");
        }

        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("user is null. please set 'org.cloudifysource.test.softlayer.api-key' "
                    + "system property and run again.");
        }

        cloud.getUser().setUser(user);
        cloud.getUser().setApiKey(apiKey);

        cloud.getProvider().setProvider("softlayer");

        ComputeTemplate computeTemplate = new ComputeTemplate();

        cloud.getCloudCompute().getTemplates().put("dummy", computeTemplate);

        return cloud;


    }
}
