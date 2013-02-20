package org.cloudifysource.dsl.internal.tools;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;

/********
 * Utility class for creating cloud related service details, used by both USM
 * and REST.
 * 
 * @author barakme
 * 
 */
public final class ServiceDetailsHelper {

	private ServiceDetailsHelper() {

	}

	/**********
	 * Utility method creating cloud related service details.
	 * 
	 * @param bindHost
	 *            the NIC cloudify binds to, used if private or public IP are
	 *            not available in the environment.
	 * @return A map with the cloud related details.
	 */
	public static Map<String, Object> createCloudDetailsMap(final String bindHost) {
		final Map<String, Object> map = new HashMap<String, Object>();

		final String privateIp = System.getenv(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP);
		final String publicIp = System.getenv(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP);

		if (privateIp != null) {
			map.put(CloudifyConstants.USM_DETAILS_PRIVATE_IP, privateIp);
		} else {
			map.put(CloudifyConstants.USM_DETAILS_PRIVATE_IP, bindHost);
		}

		if (publicIp != null) {
			map.put(CloudifyConstants.USM_DETAILS_PUBLIC_IP, publicIp);
		} else {
			map.put(CloudifyConstants.USM_DETAILS_PUBLIC_IP, bindHost);
		}

		final String imageId = System.getenv(CloudifyConstants.GIGASPACES_CLOUD_IMAGE_ID);
		if (imageId != null) {
			map.put(CloudifyConstants.USM_DETAILS_IMAGE_ID, imageId);
		}

		final String hardwareId = System.getenv(CloudifyConstants.GIGASPACES_CLOUD_HARDWARE_ID);
		if (hardwareId != null) {
			map.put(CloudifyConstants.USM_DETAILS_HARDWARE_ID, hardwareId);
		}

		final String machineId = System.getenv(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID);
		if (machineId != null) {
			map.put(CloudifyConstants.USM_DETAILS_MACHINE_ID, machineId);
		}

		return map;
	}

}
