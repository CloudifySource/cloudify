package org.cloudifysource.usm.jmx;

import java.util.Map;

import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;


public class JmxMonitor extends AbstractJmxPlugin implements Monitor {

	private static java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(AbstractJmxPlugin.class.getName());

	public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config) throws MonitorException {

		Map<String, Object> jmxAttributes = getJmxAttributes();

		return USMUtils.convertMapToNumericValues(jmxAttributes);
	}
}
