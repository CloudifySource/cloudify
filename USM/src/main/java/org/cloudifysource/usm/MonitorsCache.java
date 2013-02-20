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

package org.cloudifysource.usm;

import groovy.lang.Closure;
import groovy.lang.GString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.monitors.Monitor;
import org.openspaces.pu.service.CustomServiceDetails;
import org.openspaces.pu.service.CustomServiceMonitors;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceMonitors;

/*****************
 * A synchronized wrapper to the monitors functionality. The last monitors
 * results are cached for a set time. Note: this class also contains the code to
 * create the service details. The code for services and details is very
 * similar, even though service details is called exactly once.
 * 
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public class MonitorsCache {

	private final USMLifecycleBean lifecycleBean;

	private ServiceMonitors[] lastResult;
	private final UniversalServiceManagerBean usm;

	private final long cacheExpirationTimeout;
	private long cacheExpirationTime = 0;

	private final String serviceSubType = "USM";
	private final String serviceDescription = "USM";
	private final String serviceLongDescription = "USM";

	public MonitorsCache(final UniversalServiceManagerBean usm,
			final USMLifecycleBean lifecycleBean,
			final long cacheExpirationTimeout) {
		this.usm = usm;
		this.cacheExpirationTimeout = cacheExpirationTimeout;
		this.lifecycleBean = lifecycleBean;
	}

	/***********
	 * Returns the monitors from the cache if the cache expiration time has not
	 * been exceeded, or reads the new monitor values and caches them, returning
	 * the result.
	 * 
	 * @return the monitors.
	 */
	public synchronized ServiceMonitors[] getMonitors() {
		final long now = System.currentTimeMillis();

		if (now >= cacheExpirationTime) {
			logger.fine("Reloading monitors at: " + now);
			this.lastResult = createMonitors();
			this.cacheExpirationTime = now + cacheExpirationTimeout;
		}

		return this.lastResult;

	}

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(MonitorsCache.class.getName());

	private void removeNonSerializableObjectsFromMap(final Map<?, ?> map,
			final String mapName) {

		if (map == null || map.keySet().isEmpty()) {
			return;
		}
		final Iterator<?> entries = map.entrySet().iterator();
		while (entries.hasNext()) {
			final Entry<?, ?> entry = (Entry<?, ?>) entries.next();

			// a closure can not be serialized
			// TODO - write a unit test for this.
			if (entry.getValue() != null) {
				if (!(entry.getValue() instanceof java.io.Serializable)
						|| entry.getValue() instanceof Closure<?>) {
					logger.info("Entry "
							+ entry.getKey()
							+ " with value: "
							+ entry.getValue().toString()
							+ "  is not serializable and was not inserted to the "
							+ mapName + " map");
					entries.remove();
				}
			}
		}
	}

	private ServiceMonitors[] createMonitors() {
		final CustomServiceMonitors csm = new CustomServiceMonitors(
				CloudifyConstants.USM_MONITORS_SERVICE_ID);

		final ServiceMonitors[] res = new ServiceMonitors[] { csm };

		final USMState currentState = usm.getState();
		// If the underlying service is not running
		if (currentState != USMState.RUNNING) {
			csm.getMonitors().put(CloudifyConstants.USM_MONITORS_STATE_ID,
					currentState.ordinal());
			return res;
		}

		final Map<String, Object> map = csm.getMonitors();
		// default monitors
		putDefaultMonitorsInMap(map);

		for (final Monitor monitor : lifecycleBean.getMonitors()) {
			try {
				logger.fine("Executing monitor: " + monitor);
				final Map<String, Number> monitorValues = monitor
						.getMonitorValues(usm, lifecycleBean.getConfiguration());
				removeNonSerializableObjectsFromMap(monitorValues, "monitors");
				// add monitor values to Monitors map
				map.putAll(monitorValues);
			} catch (final Exception e) {
				logger.log(Level.SEVERE,
						"Failed to execute a USM service monitor", e);
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Monitors are: " + Arrays.toString(res));
		}

		return res;

	}

	private void putDefaultMonitorsInMap(final Map<String, Object> map) {
		map.put(CloudifyConstants.USM_MONITORS_CHILD_PROCESS_ID,
				usm.getChildProcessID());
		final List<Long> serviceProcessIDs = usm.getServiceProcessesList();
		if (serviceProcessIDs.size() > 0) {
			if (serviceProcessIDs.size() == 1) {
				map.put(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID,
						serviceProcessIDs.get(0));
			} else {
				map.put(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID,
						serviceProcessIDs);
			}
		}
		map.put(CloudifyConstants.USM_MONITORS_STATE_ID, usm.getState()
				.ordinal());
	}

	/**************
	 * Creates the service details - executed once.
	 * 
	 * @return the service details.
	 */
	public ServiceDetails[] getServicesDetails() {
		logger.fine("Executing getServiceDetails()");
		@SuppressWarnings("deprecation")
		final CustomServiceDetails csd = new CustomServiceDetails(
				CloudifyConstants.USM_DETAILS_SERVICE_ID,
				CustomServiceDetails.SERVICE_TYPE, this.serviceSubType,
				this.serviceDescription, this.serviceLongDescription);

		final ServiceDetails[] res = new ServiceDetails[] { csd };

		final Details[] alldetails = lifecycleBean.getDetails();
		final Map<String, Object> result = csd.getAttributes();
		for (final Details details : alldetails) {

			try {
				logger.fine("Executing details: " + details);
				final Map<String, Object> detailsValues = details.getDetails(
						usm, lifecycleBean.getConfiguration());
				removeNonSerializableObjectsFromMap(detailsValues, "details");
				result.putAll(detailsValues);
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "Failed to execute service details", e);
			}

		}

		// convert GStrings
		handleGStringDetails(result);

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Details are: " + Arrays.toString(res));
		}
		return res;

	}

	/**********
	 * If a details is a GString, return its toString() instead.
	 * 
	 * @param result
	 */
	private void handleGStringDetails(final Map<String, Object> result) {
		final Set<Entry<String, Object>> entries = result.entrySet();
		for (final Entry<String, Object> entry : entries) {
			if ((entry.getValue() != null)
					&& (entry.getValue() instanceof GString)) {
				entry.setValue(entry.getValue().toString());
			}
		}
	}

}
