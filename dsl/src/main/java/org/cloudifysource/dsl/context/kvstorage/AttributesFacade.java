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
package org.cloudifysource.dsl.context.kvstorage;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;


/**
 * Facade for putting and getting attributes over cloudify management space
 * @author eitany
 * @since 2.0
 */
public class AttributesFacade extends GroovyObjectSupport {

	private static final long MANAGEMENT_SPACE_FIND_TIMEOUT = 30;
	
	private final ServiceContext serviceContext;

	private final ApplicationAttributesAccessor applicationAttributesAccessor;
	private final ServiceAttributesAccessor serviceAttributesAccessor;
	//This needs to be lazy initiated because service instaceId is not available at construction time
	private InstanceAttributesAccessor instanceAttributesAccessor;
	
	private GigaSpace managementSpace;	
	private final Object managementSpaceLock = new Object();
	private final Admin admin;
	
	public AttributesFacade(ServiceContext serviceContext, Admin admin) {
		this.serviceContext = serviceContext;
		this.admin = admin;
		this.applicationAttributesAccessor = new ApplicationAttributesAccessor(this, serviceContext.getApplicationName());
		this.serviceAttributesAccessor = new ServiceAttributesAccessor(this, serviceContext.getApplicationName(), serviceContext.getServiceName(), serviceContext);		
	}
	
	GigaSpace getManagementSpace() {
		if (managementSpace != null)
			return managementSpace;
		synchronized(managementSpaceLock){
			if (managementSpace != null)
				return managementSpace;
			
			Space space = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, MANAGEMENT_SPACE_FIND_TIMEOUT, TimeUnit.SECONDS);			
			if (space == null)
				throw new IllegalStateException("No management space located");
			
			managementSpace = space.getGigaSpace();
			return managementSpace;
		}
	}
	
	public ApplicationAttributesAccessor getThisApplication(){
		return applicationAttributesAccessor;
	}
	
	public ServiceAttributesAccessor getThisService(){
		return serviceAttributesAccessor;
	}

	public InstanceAttributesAccessor getThisInstance() {
		if (instanceAttributesAccessor == null) {
			this.instanceAttributesAccessor = new InstanceAttributesAccessor(this, serviceContext.getApplicationName(), serviceContext.getServiceName(), serviceContext.getInstanceId());
		}
		return instanceAttributesAccessor;
	}
	
	@Override
	public Object getProperty(String property) {
		try {
			return super.getProperty(property);
		} catch(MissingPropertyException e) {
			return new ServiceAttributesAccessor(this, serviceContext.getApplicationName(), property, serviceContext);
		}
	}
	
}
