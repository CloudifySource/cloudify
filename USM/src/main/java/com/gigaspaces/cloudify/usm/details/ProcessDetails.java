package com.gigaspaces.cloudify.usm.details;

import java.util.HashMap;
import java.util.Map;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;


public class ProcessDetails implements Details {

	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config)
			throws DetailsException {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put("GSC PID", usm.getContainerPid());
		map.put("Working Directory", usm.getPuExtDir().getAbsolutePath());
		
		final String privateIp = System.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP);
		final String publicIp = System.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP);
		
		if(privateIp != null) {
			map.put(CloudifyConstants.USM_DETAILS_PRIVATE_IP, privateIp);
		}
		if(publicIp != null) {
			map.put(CloudifyConstants.USM_DETAILS_PUBLIC_IP, publicIp);
		}
		
		

		return map;
	}

}
