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
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;

/*******************
 * Adapter class from the deprecated cloud driver interface to the new compute driver base class.
 * 
 * @author barakme
 * @since 2.7.0
 */
@SuppressWarnings("deprecation")
public final class ComputeDriverProvisioningAdapter extends BaseComputeDriver {

	/********************
	 * Factory method for a compute driver. If the object extends the new Base Compute Drive class, returns the object.
	 * If the object implements the older, deprecated, Provisioning Driver interface, returns an adapter around it.
	 * Otherwise, throws an exception
	 * 
	 * @param driverInstance
	 *            an instance of the actual driver class.
	 * @return a Compute Driver that extends the base compute driver.
	 * @throws IllegalArgumentException
	 *             if the input does not extend BaseComputeDriver or implements ProvisioningDriver.
	 */
	public static BaseComputeDriver create(final Object driverInstance) throws IllegalArgumentException {
		if (driverInstance instanceof BaseComputeDriver) {
			return (BaseComputeDriver) driverInstance;
		}
		if (driverInstance instanceof ProvisioningDriver) {
			return new ComputeDriverProvisioningAdapter((ProvisioningDriver) driverInstance);
		}
		throw new IllegalArgumentException("Driver class is does not extend " + BaseComputeDriver.class.getName()
				+ " or implement " + ProvisioningDriver.class.getName());

	}

	private final ProvisioningDriver provisioningDriver;

	private ComputeDriverProvisioningAdapter(final ProvisioningDriver provisioningDriver) {
		this.provisioningDriver = provisioningDriver;
	}

	@Override
	public void close() {
		provisioningDriver.close();
	}

	@Override
	public void addListener(final ProvisioningDriverListener listener) {
		provisioningDriver.addListener(listener);
	}

	@Override
	public String getCloudName() {
		return provisioningDriver.getCloudName();
	}

	@Override
	public Object getComputeContext() {

		return provisioningDriver.getComputeContext();
	}

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		provisioningDriver.onServiceUninstalled(duration, unit);
	}

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		provisioningDriver.setConfig(configuration.getCloud(), configuration.getCloudTemplate(),
				configuration.isManagement(), configuration.getServiceName());
		provisioningDriver.setAdmin(configuration.getAdmin());
	}

	@Override
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException {
		ProvisioningContextAccess.setCurrentProvisioingContext(context);
		try {
			return this.provisioningDriver.startMachine(context.getLocationId(), duration, unit);
		} finally {
			// clear thread local.
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		return provisioningDriver.stopMachine(machineIp, duration, unit);
	}

	@Override
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		ProvisioningContextAccess.setCurrentManagementProvisioingContext(context);
		try {
			return provisioningDriver.startManagementMachines(duration, unit);
		} finally {
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

	@Override
	public void setProvisioningDriverClassContext(final ProvisioningDriverClassContext context) {
		if (this.provisioningDriver instanceof ProvisioningDriverClassContextAware) {
			((ProvisioningDriverClassContextAware) this.provisioningDriver).setProvisioningDriverClassContext(context);
		} else {
			super.setProvisioningDriverClassContext(context);
		}
	}

	@Override
	public void setCustomDataFile(final File customDataFile) {
		if (this.provisioningDriver instanceof CustomServiceDataAware) {
			((CustomServiceDataAware) this.provisioningDriver).setCustomDataFile(customDataFile);
		} else {
			super.setCustomDataFile(customDataFile);
		}
	}

	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		if (this.provisioningDriver instanceof ManagementLocator) {
			return ((ManagementLocator) this.provisioningDriver).getExistingManagementServers();
		} else {
			return super.getExistingManagementServers();
		}

	}

	@Override
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		if (this.provisioningDriver instanceof ManagementLocator) {
			return ((ManagementLocator) this.provisioningDriver).getExistingManagementServers(controllers);
		} else {
			return super.getExistingManagementServers(controllers);
		}

	}

	@Override
	public void validateCloudConfiguration(final ValidationContext validationContext)
			throws CloudProvisioningException {
		if (this.provisioningDriver instanceof ProvisioningDriverBootstrapValidation) {
			((ProvisioningDriverBootstrapValidation) this.provisioningDriver)
					.validateCloudConfiguration(validationContext);
		} else {
			super.validateCloudConfiguration(validationContext);
		}
	}
	
	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {
		this.provisioningDriver.stopManagementMachines();
	}

}
