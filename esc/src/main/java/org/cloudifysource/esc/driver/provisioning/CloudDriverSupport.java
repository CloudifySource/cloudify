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

package org.cloudifysource.esc.driver.provisioning;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;

/**************
 * An abstract class with some commonly used code for custom cloud drivers.
 * 
 * @author barakme
 * @since 2.1
 * 
 */
public abstract class CloudDriverSupport implements ProvisioningDriver {

	protected final List<ProvisioningDriverListener> listeners = new LinkedList<ProvisioningDriverListener>();
	protected Admin singleThreadedAdmin;
	private Admin multiThreadedAdmin;
	protected Cloud cloud;
	protected boolean management;
	protected String templateName;
	protected ComputeTemplate template;

	// maps ip to time when last shut down request for that machine was sent
	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();

	private static final int MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT = 120000;

	protected static final Logger logger = Logger.getLogger(CloudDriverSupport.class.getName());

	@Override
	public void addListener(final ProvisioningDriverListener listener) {
		listeners.add(listener);
	}

	@Override
	public void setAdmin(final Admin admin) {
		this.singleThreadedAdmin = admin;
	}

	/***********
	 * Returns a multithreaded admin instance, which can be used for blocking operations without blocking the shared
	 * Admin instance provided by the ESM. The method is synchronized to prevent several threads initializing the multi
	 * threaded admin.
	 * 
	 * TODO: allow loading multi threaded admin into ESM context.
	 * 
	 * @return the multi threaded admin instance.
	 */
	protected synchronized Admin getMultiThreadedAdmin() {
		if (this.multiThreadedAdmin != null) {
			return this.multiThreadedAdmin;
		}

		final AdminFactory factory = new AdminFactory();
		final String[] groups = singleThreadedAdmin.getGroups();
		for (final String group : groups) {
			factory.addGroup(group);
		}

		final LookupLocator[] locators = singleThreadedAdmin.getLocators();
		for (final LookupLocator lookupLocator : locators) {
			factory.addLocator(lookupLocator.toString());
		}

		this.multiThreadedAdmin = factory.createAdmin();
		return this.multiThreadedAdmin;
	}

	@Override
	public void setConfig(final Cloud cloud, final String templateName, final boolean management, final String serviceName) {
		
		this.cloud = cloud;
		this.management = management;
		this.templateName = templateName;
		
		if (this.cloud.getCloudCompute().getTemplates().isEmpty()) {
			throw new IllegalArgumentException("No templates defined for this cloud");
		}

		// TODO - add automatic validation rules to the DSL Pojos!
		if (StringUtils.isBlank(this.templateName)) {
			this.template = this.cloud.getCloudCompute().getTemplates().values().iterator().next();
		} else {
			this.template = this.cloud.getCloudCompute().getTemplates().get(this.templateName);
		}

		if (this.template == null) {
			throw new IllegalArgumentException("Could not find required template: " + this.templateName
					+ " in templates list");
		}
	}

	
	/*********
	 * Checks if a stop request for this machine was already requested recently.
	 * @param ip ip of the machine.
	 * @return true if there was a recent request, false otherwise.
	 */
	protected boolean isStopRequestRecent(final String ip) {
		// TODO - move this to the adapter!
		final Long previousRequest = stoppingMachines.get(ip);
		if (previousRequest != null
				&& System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT) {
			logger.fine("Machine " + ip + " is already stopping. Ignoring this shutdown request");
			return true;
		}

		// TODO - add a task thkat cleans up this map
		stoppingMachines.put(ip, System.currentTimeMillis());
		return false;
	}

}
