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
package org.cloudifysource.dsl.internal.context;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.kvstorage.AttributesFacade;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.cluster.ClusterInfo;

/**
 *
 *
 * @author barakme
 * @since 1.0
 */
public class ServiceContextImpl implements ServiceContext {

	private org.cloudifysource.dsl.Service service;
	private Admin admin;
	private final String serviceDirectory;
	private ClusterInfo clusterInfo;
	private boolean initialized = false;

	private String serviceName;

	private String applicationName;

	private AttributesFacade attributesFacade;

	// TODO - this property should not be settable - there should be a separate
	// interface for that.
	// this pid may be modified due to process crashed, so volatile is required.
	private volatile long externalProcessId;

	/*************
	 * Constructor.
	 *
	 * @param clusterInfo
	 *            the cluster info.
	 * @param serviceDirectory
	 *            the service directory.
	 *
	 */
	public ServiceContextImpl(final ClusterInfo clusterInfo,
			final String serviceDirectory) {
		if (clusterInfo == null) {
			throw new NullPointerException(
					"Cluster Info provided to service context cannot be null!");
		}
		this.clusterInfo = clusterInfo;
		this.serviceDirectory = serviceDirectory;
		if (clusterInfo.getName() != null) {
			FullServiceName fullName = ServiceUtils
					.getFullServiceName(clusterInfo.getName());
			this.applicationName = fullName.getApplicationName();
			this.serviceName = fullName.getServiceName();
		}

	}

	/**********
	 * Late object initialization.
	 *
	 * @param service
	 *            .
	 * @param admin
	 *            .
	 * @param clusterInfo
	 *            .
	 */
	public void init(final Service service, final Admin admin,
			final ClusterInfo clusterInfo) {
		this.service = service;
		this.admin = admin;

		// TODO - is the null path even possible?
		if (clusterInfo == null) {
			this.applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
			this.serviceName = service.getName();
		} else {
			logger.fine("Parsing full service name from PU name: "
					+ clusterInfo.getName());
			final FullServiceName fullServiceName = ServiceUtils
					.getFullServiceName(clusterInfo.getName());
			logger.fine("Got full service name: " + fullServiceName);
			this.serviceName = fullServiceName.getServiceName();
			this.applicationName = fullServiceName.getApplicationName();

		}
		if (admin != null) {
			final boolean found = this.admin.getLookupServices().waitFor(1, 30,
					TimeUnit.SECONDS);
			if (!found) {
				throw new AdminException(
						"A service context could not be created as the Admin API could not find a lookup service "
								+ "in the network, using groups: "
								+ Arrays.toString(admin.getGroups())
								+ " and locators: "
								+ Arrays.toString(admin.getLocators()));
			}
		}
		this.attributesFacade = new AttributesFacade(this, admin);
		initialized = true;
	}

	/************
	 * Late initializer, used in the integrated container (i.e. test-recipe)
	 *
	 * @param service
	 *            .
	 */
	public void initInIntegratedContainer(final Service service) {
		this.service = service;

		this.clusterInfo = new ClusterInfo(null, 1, 0, 1, 0);
		if (service != null) {
			this.clusterInfo.setName(service.getName());
			this.serviceName = service.getName();
		}

		this.applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;

		this.attributesFacade = new AttributesFacade(this, admin);
		initialized = true;

	}

	private void checkInitialized() {
		if (!this.initialized) {
			throw new IllegalStateException(
					"The Service Context has not been initialized yet. "
							+ "It can only be used after the Service file has been fully evaluated");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.dsl.context.IServiceContext#getInstanceId()
	 */
	@Override
	public int getInstanceId() {
		// checkInitialized();

		return clusterInfo.getInstanceId();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.cloudifysource.dsl.context.IServiceContext#waitForService(java.lang
	 * .String, int, java.util.concurrent.TimeUnit)
	 */
	@Override
	public org.cloudifysource.dsl.context.Service waitForService(
			final String name, final int timeout, final TimeUnit unit) {
		checkInitialized();

		if (this.admin != null) {
			final String puName = ServiceUtils.getAbsolutePUName(
					this.applicationName, name);
			final ProcessingUnit pu = waitForProcessingUnitFromAdmin(puName,
					timeout, unit);
			if (pu == null) {
				return null;
			} else {
				return new org.cloudifysource.dsl.internal.context.ServiceImpl(
						pu);
			}
		}

		// running in integrated container
		if (name.equals(this.service.getName())) {
			return new org.cloudifysource.dsl.internal.context.ServiceImpl(
					name, service.getNumInstances());
		}

		throw new IllegalArgumentException(
				"When running in the integrated container, Service Context only includes the running service");

	}

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceContextImpl.class.getName());

	private ProcessingUnit waitForProcessingUnitFromAdmin(final String name,
			final long timeout, final TimeUnit unit) {

		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(name,
				timeout, unit);
		if (pu == null) {
			logger.warning("Processing unit with name: "
					+ name
					+ " was not found in the cluster. Are you running in an IntegratedProcessingUnitContainer? "
					+ "If not, consider extending the timeout.");
		}

		return pu;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.dsl.context.IServiceContext#getServiceDirectory()
	 */
	@Override
	public String getServiceDirectory() {

		return serviceDirectory;
	}

	/**
	 * Returns the Admin Object the underlies the Service Context. Note: this is
	 * intended as a debugging aid, and should not be used by most application.
	 * Only power users, familiar with the details of the Admin API, should use
	 * it.
	 *
	 * @return the admin.
	 */
	public Admin getAdmin() {
		return admin;
	}

	/**
	 *
	 * @param service
	 */
	void setService(final Service service) {
		this.service = service;
	}

	public ClusterInfo getClusterInfo() {
		return clusterInfo;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.dsl.context.IServiceContext#getServiceName()
	 */
	@Override
	public String getServiceName() {
		return serviceName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.dsl.context.IServiceContext#getApplicationName()
	 */
	@Override
	public String getApplicationName() {
		return applicationName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.dsl.context.IServiceContext#getAttributes()
	 */
	@Override
	public AttributesFacade getAttributes() {
		return attributesFacade;
	}

	@Override
	public String toString() {
		if (this.initialized) {
			return "ServiceContext [dir=" + serviceDirectory + ", clusterInfo="
					+ clusterInfo + "]";
		} else {
			return "ServiceContext [NOT INITIALIZED]";
		}
	}

	@Override
	public long getExternalProcessId() {
		return externalProcessId;
	}

	public void setExternalProcessId(final long externalProcessId) {
		this.externalProcessId = externalProcessId;
	}

	@Override
	public boolean isLocalCloud() {
		return IsLocalCloudUtils.isLocalCloud();
	}

	@Override
	public String getPublicAddress() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP);
		if (envVar != null) {
			return envVar;
		}

		return ServiceUtils.getPrimaryInetAddress();

	}

	@Override
	public String getPrivateAddress() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP);
		if (envVar != null) {
			return envVar;
		}

		return ServiceUtils.getPrimaryInetAddress();
	}

	@Override
	public String getImageID() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_CLOUD_IMAGE_ID);

		return envVar;
	}

	@Override
	public String getHardwareID() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_CLOUD_HARDWARE_ID);

		return envVar;
	}

	@Override
	public String getCloudTemplateName() {
		final String envVar = System
				.getenv(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME);

		return envVar;
	}

	@Override
	public String getMachineID() {
		final String envVar = System
				.getenv(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID);

		return envVar;
	}

}
