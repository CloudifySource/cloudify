package org.cloudifysource.dsl.internal.tools;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;

public final class ServiceDetailsHelper {

	private ServiceDetailsHelper() {

	}

	public static Map<String, Object> createCloudDetailsMap(
			final String bindHost) {
		final Map<String, Object> map = new HashMap<String, Object>();

		final String privateIp = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP);
		final String publicIp = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP);

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

		final String imageId = System
				.getenv(CloudifyConstants.CLOUDIFY_CLOUD_IMAGE_ID);
		if (imageId != null) {
			map.put(CloudifyConstants.USM_DETAILS_IMAGE_ID, imageId);
		}

		final String hardwareId = System
				.getenv(CloudifyConstants.CLOUDIFY_CLOUD_HARDWARE_ID);
		if (hardwareId != null) {
			map.put(CloudifyConstants.USM_DETAILS_HARDWARE_ID, hardwareId);
		}
		return map;
	}

}
