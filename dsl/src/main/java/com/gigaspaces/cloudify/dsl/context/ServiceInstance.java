package com.gigaspaces.cloudify.dsl.context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.logging.Level;

import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceMonitors;

public class ServiceInstance {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceInstance.class.getName());
	private final ProcessingUnitInstance pui;

	ServiceInstance(final ProcessingUnitInstance pui) {
		this.pui = pui;
	}

	public int getInstanceID() {
		if (pui != null) {
			return pui.getInstanceId();
		} else {
			return 1;
		}
	}

	public String getHostAddress() {
		if (pui != null) {
			return pui.getMachine().getHostAddress();
		} else {
			try {
				return InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				logger.log(Level.SEVERE, "Failed to read local host address", e);
				return null;
			}
		}
	}

	public String getHostName() {

		if (pui != null) {
			return pui.getMachine().getHostName();
		} else {
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				logger.log(Level.SEVERE, "Failed to read local host address", e);
				return null;
			}
		}
	}

	public void invoke(String commandName) {
		throw new UnsupportedOperationException("Invoke not supported yet!");
	}

	@Override
	public String toString() {
		return "ServiceInstance [getInstanceID()=" + getInstanceID()
				+ ", getHostAddress()=" + getHostAddress() + ", getHostName()="
				+ getHostName() + "]";
	}

	/**********
	 * Returns the details for the given service ID and key.
	 * 
	 * @param serviceDetailsId
	 *            the service ID.
	 * @param serviceDetailsKey
	 *            the details key.
	 * @return the details result, which may be null if the key does not exist
	 *         in the service details.
	 * @throws IllegalArgumentException
	 *             if the service ID is not found.
	 */
//	public Object getDetails(String serviceDetailsId, String serviceDetailsKey) {
//		if (this.pui == null) { // running in integrated container
//			return null;
//		}
//
//		ServiceDetails details = this.pui
//				.getServiceDetailsByServiceId(serviceDetailsId);
//		if (details == null) {
//			throw new IllegalArgumentException(
//					"No details found with service ID: " + serviceDetailsId);
//		}
//		return details.getAttributes().get(serviceDetailsKey);
//	}

	/**********
	 * Returns the details for the given key by iterating over all service
	 * details in this processing unit instances and returning the first match.
	 * 
	 * @param serviceDetailsKey
	 *            the details key.
	 * @return the details result, which may be null if the key does not exist
	 *         in the service details.
	 */
	public Object getDetails(String serviceDetailsKey) {
		if (this.pui == null) { // running in integrated container
			return null;
		}

		Collection<ServiceDetails> allDetails = this.pui
				.getServiceDetailsByServiceId().values();
		for (ServiceDetails serviceDetails : allDetails) {
			Object res = serviceDetails.getAttributes().get(serviceDetailsKey);
			if (res != null) {
				return res;
			}

		}
		return null;
	}

	/**********
	 * Returns the monitor value for the given service ID and key.
	 * 
	 * @param serviceMonitorsId
	 *            the service ID.
	 * @param serviceMonitorsKey
	 *            the monitor key.
	 * @return the result, which may be null if the key does not exist in the
	 *         service monitors.
	 * @throws IllegalArgumentException
	 *             if the service ID is not found.
	 */
//	public Object getMonitors(String serviceMonitorsId,
//			String serviceMonitorsKey) {
//		if (this.pui == null) { // running in integrated container
//			return null;
//		}
//
//		Map<String, ServiceMonitors> monitors = this.pui.getStatistics()
//				.getMonitors();
//		ServiceMonitors monitor = monitors.get(serviceMonitorsId);
//		if (monitor == null) {
//			throw new IllegalArgumentException("No monitors found with ID: "
//					+ serviceMonitorsId);
//		}
//		return monitor.getMonitors().get(serviceMonitorsKey);
//
//	}

	/**********
	 * Returns the monitor for the given key by iterating over all service
	 * monitor in this processing unit instances and returning the first match.
	 * 
	 * @param serviceMonitorsKey
	 *            the details key.
	 * @return the monitor result, which may be null if the key does not exist
	 *         in the service details.
	 */
	public Object getMonitors(String serviceMonitorsKey) {
		if (this.pui == null) { // running in integrated container
			return null;
		}

		Collection<ServiceMonitors> allMonitors = this.pui.getStatistics()
				.getMonitors().values();

		for (ServiceMonitors serviceMonitors : allMonitors) {
			Object res = serviceMonitors.getMonitors().get(serviceMonitorsKey);
			if (res != null) {
				return res;
			}

		}

		return null;
	}

}
