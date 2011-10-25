package com.gigaspaces.cloudify.usm.monitors.process;

import java.util.Map;

import org.hyperic.sigar.Sigar;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.monitors.MonitorException;
import com.gigaspaces.internal.sigar.SigarHolder;

public class ProcessMonitor implements Monitor {

	private final Sigar sigar = SigarHolder.getSigar();

	
	public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config) throws MonitorException {
		
		final MonitorData data = new MonitorData(sigar, usm.getActualProcessID());
		final Map<String, Number> map = data.getDataMap();
		return map;

	}

}
