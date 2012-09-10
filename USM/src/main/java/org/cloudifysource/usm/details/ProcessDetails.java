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
package org.cloudifysource.usm.details;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;

import com.gigaspaces.lrmi.nio.info.NIOInfoHelper;

/**************
 * Process level static data exposed to client.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class ProcessDetails implements Details {

	@Override
	public Map<String, Object> getDetails(
			final UniversalServiceManagerBean usm,
			final ServiceConfiguration config) throws DetailsException {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put("GSC PID", usm.getContainerPid());
		map.put("Working Directory", usm.getPuExtDir().getAbsolutePath());

		String bindHost = null;
		if (usm.isRunningInGSC()) {
			bindHost = NIOInfoHelper.getDetails().getBindHost();
		} else {
			// running in integrated container, so use default value.
			bindHost = "127.0.0.1";
		}
		addCloudDetailsToMap(map, bindHost);

		return map;
	}

	public void addCloudDetailsToMap(final Map<String, Object> map,
			String bindHost) {
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
	}

}
