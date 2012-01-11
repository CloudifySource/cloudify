package org.cloudifysource.usm.monitors.process;

import java.util.Map;

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;
import org.hyperic.sigar.Sigar;

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
