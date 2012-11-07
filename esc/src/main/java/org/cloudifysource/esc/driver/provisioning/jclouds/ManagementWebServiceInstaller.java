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
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;

import com.j_spaces.kernel.Environment;

public class ManagementWebServiceInstaller {

	private static final String TIMEOUT_ERROR_MESSAGE = "operation timed out waiting for the rest service to start";

	private final static Logger logger = Logger.getLogger(ConditionLatch.class.getName());
	private Admin admin;
	private boolean verbose;
	private long progressInSeconds;
	private int memoryInMB;
	private int port;
	private File warFile;
	private String serviceName;
	private String zone;

	private static final int RESERVED_MEMORY_IN_MB = 256;
	public static final String MANAGEMENT_APPLICATION_NAME = "management";

	public void setProgress(final int progress, final TimeUnit timeunit) {
		this.progressInSeconds = timeunit.toSeconds(progress);
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public void setMemory(final long memory, final MemoryUnit unit) {
		this.memoryInMB = (int) unit.toMegaBytes(memory);
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setWarFile(final File warFile) {
		this.warFile = warFile;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public void setManagementZone(final String zone) {
		this.zone = zone;
	}

	public void install()
			throws TimeoutException, InterruptedException, CloudProvisioningException,
			ProcessingUnitAlreadyDeployedException {

		if (zone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}

		final ElasticStatelessProcessingUnitDeployment deployment =
				new ElasticStatelessProcessingUnitDeployment(getGSFile(warFile))
						.memoryCapacityPerContainer(memoryInMB,
								MemoryUnit.MEGABYTES)
						.name(serviceName)
						// All PUs on this role share the same machine. Machines
						// are identified by zone.
						.sharedMachineProvisioning("public",
								new DiscoveredMachineProvisioningConfigurer().addGridServiceAgentZone(zone)
										.reservedMemoryCapacityPerMachine(RESERVED_MEMORY_IN_MB,
												MemoryUnit.MEGABYTES).create())
						// Eager scale (1 container per machine per PU)
						.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());

		for (final Entry<Object, Object> prop : getContextProperties().entrySet()) {
			deployment.addContextProperty(prop.getKey().toString(),
					prop.getValue().toString());
		}
		getGridServiceManager().deploy(deployment);
	}

	private GridServiceManager getGridServiceManager()
			throws CloudProvisioningException {
		final Iterator<GridServiceManager> it = admin.getGridServiceManagers().iterator();
		if (it.hasNext()) {
			return it.next();
		}
		throw new CloudProvisioningException("No Grid Service Manager found to deploy " + serviceName);
	}

	public URL waitForProcessingUnitInstance(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)

			throws InterruptedException, TimeoutException, CloudProvisioningException {

		createConditionLatch(timeout,
				timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone()
					throws CloudProvisioningException, InterruptedException {
				logger.info("Waiting for " + serviceName + " service.");
				final ProcessingUnit pu = getProcessingUnit();
				boolean isDone = false;
				if (pu != null) {
					for (final ProcessingUnitInstance instance : pu) {
						if (agent.equals(instance.getGridServiceContainer().getGridServiceAgent())) {
							isDone = true;
							break;
						}
					}
				}
				return isDone;
			}
		});

		final URL url = getWebProcessingUnitURL(agent,
				getProcessingUnit());
		if (logger.isLoggable(Level.INFO)) {
			final String serviceNameCapital = new StringBuilder(serviceName).replace(0,
					1,
					serviceName.substring(0,
							1).toUpperCase()).toString();
			logger.info(serviceNameCapital + " service is available at: " + url);
		}
		return url;
	}

	private Properties getContextProperties() {
		final Properties props = new Properties();
		props.setProperty("com.gs.application",
				MANAGEMENT_APPLICATION_NAME);
		props.setProperty("web.port",
				String.valueOf(port));
		props.setProperty("web.context",
				"/");
		props.setProperty("web.context.unique",
				"true");
		return props;
	}

	public void waitForManagers(final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {

		createConditionLatch(timeout,
				timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone()
					throws CloudProvisioningException, InterruptedException {

				boolean isDone = true;
				if (0 == admin.getGridServiceManagers().getSize()) {
					isDone = false;
					if (verbose) {
						logger.info("Waiting for Grid Service Manager");
					}
				}

				if (admin.getElasticServiceManagers().getSize() == 0) {
					isDone = false;
					if (verbose) {
						logger.info("Waiting for Elastic Service Manager");
					}
				}

				if (!isDone && !verbose) {
					logger.info("Waiting for Cloudify management processes");
				}

				return isDone;
			}
		});

		admin.getGridServiceManagers().waitForAtLeastOne();
	}

	private ConditionLatch createConditionLatch(final long timeout, final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout,
				timeunit).pollingInterval(progressInSeconds,
				TimeUnit.SECONDS).timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE).verbose(verbose);
	}

	private ProcessingUnit getProcessingUnit() {
		return admin.getProcessingUnits().getProcessingUnit(serviceName);
	}

	public static URL getWebProcessingUnitURL(final GridServiceAgent agent, final ProcessingUnit pu) {
		ProcessingUnitInstance pui = null;

		for (final ProcessingUnitInstance instance : pu.getInstances()) {
			if (instance.getGridServiceContainer() != null
					&& instance.getGridServiceContainer().getGridServiceAgent() != null
					&& instance.getGridServiceContainer().getGridServiceAgent().equals(agent)) {
				pui = instance;
			}
		}

		if (pui == null) {
			throw new IllegalStateException("Failed finding " + pu.getName() + " on "
					+ agent.getMachine().getHostAddress());
		}

		final Map<String, ServiceDetails> alldetails = pui.getServiceDetailsByServiceId();

		final ServiceDetails details = alldetails.get("jee-container");
		final String host = details.getAttributes().get("host").toString();
		final String port = details.getAttributes().get("port").toString();
		final String ctx = details.getAttributes().get("context-path").toString();
		final String url = "http://" + host + ":" + port + ctx;
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			// this is a bug since we formed the URL correctly
			throw new IllegalStateException(e);
		}
	}

	public static File getGSFile(File warFile) {
		if (!warFile.isAbsolute()) {
			warFile = new File(Environment.getHomeDirectory(), warFile.getPath());
		}
		return warFile;
	}

}
