/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.dsl.context;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.context.kvstorage.AttributesFacade;

/***********
 * Context interface, available in the service recipe.
 * 
 * @author barakme
 * 
 */
public interface ServiceContext {

	/**
	 * Returns the instance ID of this instance in the cluster.
	 * 
	 * @return the instance ID.
	 */
	int getInstanceId();

	/********
	 * Waits for the specified period of time until the service with the given name becomes available.
	 * 
	 * @param name
	 *            the service name.
	 * @param timeout
	 *            the timeout.
	 * @param unit
	 *            the unit of time used with the timeout.
	 * @return the Service.
	 */
	Service waitForService(final String name, final int timeout, final TimeUnit unit);

	/**
	 * The service folder for the current service instance.
	 * 
	 * @return the service directory.
	 */
	String getServiceDirectory();

	/********************
	 * Returns the name of the current service.
	 * 
	 * @return the service name.
	 */
	String getServiceName();

	/**************
	 * Returns the current application name.
	 * 
	 * @return the application name.
	 */
	String getApplicationName();

	/*****************
	 * Access to the attributes-store.
	 * 
	 * @return access to the attributes-store.
	 */
	AttributesFacade getAttributes();

	/************
	 * Returns the process ID of the monitored process.
	 * 
	 * @return the process ID of the monitored process.
	 */
	long getExternalProcessId();
	

	/**
	 * @return true if running on localcloud, false otherwise
	 * @since 2.1
	 */
	boolean isLocalCloud();
	
	/*********
	 * Returns the current machine's public address.
	 * 
	 * @return the current machine's public address.
	 */
	String getPublicAddress();
	
	/*********
	 * Returns the current machine's private address.
	 * 
	 * @return the current machine's private address.
	 */
	String getPrivateAddress();
	
	/*********
	 * Returns the current machine's Cloud image ID.
	 * 
	 * @return the current machine's Cloud image ID.
	 */
	String getImageID();
	
	/*********
	 * Returns the current machine's Cloud hardware ID.
	 * 
	 * @return the current machine's Cloud hardware ID.
	 */
	String getHardwareID();
	
	/*********
	 * Returns the current machine's Cloud template name.
	 * 
	 * @return the current machine's Cloud template name.
	 */
	String getCloudTemplateName();
	
	/*********
	 * Returns the current machine's ID.
	 * 
	 * @return the current machine's ID.
	 */
	String getMachineID();


}