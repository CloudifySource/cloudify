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
package org.cloudifysource.usm.monitors.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;
import org.hyperic.sigar.Sigar;

import com.gigaspaces.internal.sigar.SigarHolder;

/*************
 * Monitor implementation that collects operating system metrics using SIGAR.
 * 
 * @author barakme
 * @since 2.1.0
 * 
 */
public class ProcessMonitor implements Monitor {

	private final Sigar sigar = SigarHolder.getSigar();

	@Override
	public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
			final ServiceConfiguration config)
			throws MonitorException {

		final List<Long> pids = usm.getServiceProcessesList();

		if (pids.isEmpty()) {
			return new HashMap<String, Number>();
		} else if (pids.size() == 1) {
			final MonitorData data = new MonitorData(sigar, pids.get(0));
			final Map<String, Number> map = data.getDataMap();
			return map;
		} else {
			// Collect data for all processes, add PID to key name, and return all in one map.
			final Map<String, Number> allProcessesMap = new HashMap<String, Number>();
			for (final Long pid : pids) {
				final MonitorData data = new MonitorData(sigar, pid);
				final Map<String, Number> map = data.getDataMap();
				final Set<Entry<String, Number>> entries = map.entrySet();
				final String postfix = "-" + pid;
				for (final Entry<String, Number> entry : entries) {
					allProcessesMap.put(entry.getKey() + postfix, entry.getValue());
				}

			}

			return allProcessesMap;

		}

	}

}
