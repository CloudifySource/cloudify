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

package org.cloudifysource.esc.driver.provisioning.jclouds.softlayer;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.softlayer.compute.functions.guest.VirtualGuestToNodeMetadata;
import org.jclouds.softlayer.compute.functions.guest.VirtualGuestToReducedNodeMetaData;
import org.jclouds.softlayer.reference.SoftLayerConstants;

import java.util.Set;

/**
 * This driver injects a custom module to jclouds in order to boost poor performance on softlayer.
 *
 * @author Eli Polonsky
 * @since 2.7.0
 */

public class SoftlayerProvisioningDriver extends DefaultProvisioningDriver {

    @Override
    public Set<Module> setupModules(final String templateName, final ComputeTemplate template) {
        Set<Module> modules = super.setupModules(templateName, template);
        int packageId = Utils.getInteger(template.getOverrides()
                .get(SoftLayerConstants.PROPERTY_SOFTLAYER_PACKAGE_ID), 46);
        if (packageId == 46) { // We are using virtual guests
            modules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VirtualGuestToNodeMetadata.class).to(VirtualGuestToReducedNodeMetaData.class);
                }
            });
        }
        return modules;
    }
}
