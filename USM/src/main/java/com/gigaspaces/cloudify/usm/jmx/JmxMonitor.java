package com.gigaspaces.cloudify.usm.jmx;

import java.util.Map;

import com.gigaspaces.cloudify.usm.USMUtils;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.monitors.MonitorException;

public class JmxMonitor extends AbstractJmxPlugin implements Monitor {

	private static java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(AbstractJmxPlugin.class.getName());

	public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config) throws MonitorException {

		Map<String, Object> jmxAttributes = getJmxAttributes();

		return USMUtils.convertMapToNumericValues(jmxAttributes);
	}
}
