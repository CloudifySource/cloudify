package com.gigaspaces.cloudify.usm.details;

import java.util.HashMap;
import java.util.Map;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;


public class ProcessDetails implements Details {

	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config)
			throws DetailsException {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put("GSC PID", usm.getContainerPid());
		map.put("Working Directory", usm.getPuExtDir().getAbsolutePath());
		

		return map;
	}

}
