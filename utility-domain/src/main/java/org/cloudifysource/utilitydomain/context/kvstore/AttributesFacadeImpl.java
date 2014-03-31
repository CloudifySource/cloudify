/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.utilitydomain.context.kvstore;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.kvstorage.AttributesFacade;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;

/**
 * Facade for putting and getting attributes over cloudify management space.
 * 
 * @author eitany
 * @since 2.0
 */
public class AttributesFacadeImpl extends GroovyObjectSupport implements AttributesFacade {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(AttributesFacadeImpl.class.getName());
	
	private final ServiceContext serviceContext;
	private final ApplicationAttributesAccessor applicationAttributesAccessor;
	private final ServiceAttributesAccessor serviceAttributesAccessor;
	private final GlobalAttributesAccessor globalAttributesAccessor;
	// This needs to be lazy initiated because service instaceId is not available at construction time
	private InstanceAttributesAccessor instanceAttributesAccessor;

	private volatile GigaSpace managementSpace;
	private final Object managementSpaceLock = new Object();
	private final Admin admin;

	public AttributesFacadeImpl(final ServiceContext serviceContext, final Admin admin) {
		this.serviceContext = serviceContext;
		this.admin = admin;
		this.applicationAttributesAccessor =
				new ApplicationAttributesAccessor(this, serviceContext.getApplicationName());
		this.serviceAttributesAccessor =
				new ServiceAttributesAccessor(this, serviceContext.getApplicationName(),
						serviceContext.getServiceName(), serviceContext);
		this.globalAttributesAccessor = 
				new GlobalAttributesAccessor(this);
	}

    public ApplicationAttributesAccessor getThisApplication() {
        return applicationAttributesAccessor;
    }

    public GlobalAttributesAccessor getGlobal() {
        return this.globalAttributesAccessor;
    }

    public ServiceAttributesAccessor getThisService() {
        return serviceAttributesAccessor;
    }

    public InstanceAttributesAccessor getThisInstance() {
        if (instanceAttributesAccessor == null) {
            this.instanceAttributesAccessor =
                    new InstanceAttributesAccessor(this, serviceContext.getApplicationName(),
                            serviceContext.getServiceName(), serviceContext.getInstanceId());
        }
        return instanceAttributesAccessor;
    }

    @Override
    public Object getProperty(final String property) {
        try {
            return super.getProperty(property);
        } catch (final MissingPropertyException e) {
            return new ServiceAttributesAccessor(this, serviceContext.getApplicationName(), property, serviceContext);
        }
    }

	public GigaSpace getManagementSpace() {
		if (managementSpace != null) {
			return managementSpace;
		}
		synchronized (managementSpaceLock) {
			if (managementSpace != null) {
				return managementSpace;
			}

			Space space = null;
			long timeout = getAttributesStoreDiscoveryTimeout();
			logger.finest("attempting to get attributes store, timeout: " + timeout);
			for (int i = 0; i < CloudifyConstants.MANAGEMENT_SPACE_FIND_REPEAT; ++i) {
				space = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME,
						timeout, TimeUnit.SECONDS);
				if (space != null) {
					break;
				}
			}
			if (space == null) {
				// see GS-8475 - retry one last time - if still null, throw exception
				space = admin.getSpaces().getSpaceByName(CloudifyConstants.MANAGEMENT_SPACE_NAME);
				if (space == null) {
					throw new IllegalStateException("No management space located");
				}
			}

			managementSpace = space.getGigaSpace();
			return managementSpace;
		}
	}
	
	private long getAttributesStoreDiscoveryTimeout() {

		long discoveryTimeout;
		
		final long DEFAULT_TIMEOUT = CloudifyConstants.MANAGEMENT_SPACE_FIND_TIMEOUT;
		
		String envVar = serviceContext.getAttributesStoreDiscoveryTimeout();
		if (envVar == null) {
			logger.info("Could not find environment variable: " 
					+ CloudifyConstants.USM_ATTRIBUTES_STORE_DISCOVERY_TIMEOUT_ENV_VAR + ". Using default value: " 
					+ DEFAULT_TIMEOUT + " instead.");
			envVar = "" + DEFAULT_TIMEOUT;
		}
		
		try {
			discoveryTimeout = Long.parseLong(envVar);
		} catch (final NumberFormatException nfe) {
			logger.warning("Failed to parse integer environment variable: "
					+ CloudifyConstants.USM_ATTRIBUTES_STORE_DISCOVERY_TIMEOUT_ENV_VAR + ". Value was: " + envVar 
					+ ". Using default value " + DEFAULT_TIMEOUT + " instead");
			discoveryTimeout = DEFAULT_TIMEOUT;
		}

		return discoveryTimeout;
	}
	
}
