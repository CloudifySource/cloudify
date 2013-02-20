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
import java.util.Map;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.monitors.MonitorException;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;

import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/**
 * Adds monitor targets to be polled by the JMX monitor thread.
 * 
 * @author giladh, barakme
 * @since 1.0.
 * 
 */
public class MonitorData {

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(MonitorData.class.getName());

	public MonitorData(final Sigar sigar, final long pid) throws MonitorException {
		try {
			gatherData(sigar, pid);
		} catch (final SigarException e) {
			final String msg = "Failed to read external process data via Sigar: " + e;
			logger.severe(msg);
			throw new MonitorException(msg, e);
		}
	}

	private void gatherData(final Sigar sigar, final long pid)
			throws SigarException {

		try {
			final ProcCpu pcpu = sigar.getProcCpu(pid);
			processCpuUsage = pcpu.getPercent();
			processCpuKernelTime = pcpu.getSys();
			totalProcessCpuTime = pcpu.getTotal(); // sum of users+sys
		} catch (final SigarException e) {
			logger.log(Level.FINE, "Failed to gather process info from Sigar: " + e.getMessage(), e);
		}
		

		try {
			final ProcCred prcred = sigar.getProcCred(pid);
			processGroupId = prcred.getGid();
			processUserId = prcred.getUid();
		} catch (final SigarException e) {
			logger.log(Level.FINE, "Failed to gather process info from Sigar: " + e.getMessage(), e);
		}

	
		try {
			final ProcMem pmem = sigar.getProcMem(pid);
			totalNumOfPageFaults = pmem.getPageFaults();
			totalProcessResidentalMemory = pmem.getResident();
			totalProcessSharedMemory = pmem.getShare();
			totalProcessVirtualMemory = pmem.getSize();
		} catch (final SigarException e) {
			logger.log(Level.FINE, "Failed to gather process info from Sigar: " + e.getMessage(), e);
		}

		try {
			final ProcState prcstat = sigar.getProcState(pid);
			kernelSchedulingPriority = prcstat.getPriority();
			numOfActiveThreads = prcstat.getThreads();
		} catch (final SigarException e) {
			logger.log(Level.FINE, "Failed to gather process info from Sigar: " + e.getMessage(), e);

		}

	}

	/*******
	 * Returns the collected information.
	 * @return the collection information.
	 */
	public Map<String, Number> getDataMap() {
		final Map<String, Number> map = new HashMap<String, Number>();
		addDataToMonitor(map);
		return map;
	}

	private void addDataToMonitor(final Map<String, Number> monitorMap) {
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_CPU_USAGE, processCpuUsage);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_CPU_KERNEL_TIME, processCpuKernelTime);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_CPU_TIME, totalProcessCpuTime);

		// monitorMap.put("Process Arguments", MonitorData.flattenStrArr(processArguments));

		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_GROUP_ID, processGroupId);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_USER_ID, processUserId);

		// monitorMap.put("Process Owner Group Name", MonitorData.safeS(processOwnerGroupName));
		// monitorMap.put("Process Owner User Name", MonitorData.safeS(processOwnerUserName));

		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_PAGE_FAULTS, totalNumOfPageFaults);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_RESIDENTAL_MEMORY, totalProcessResidentalMemory);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_SHARED_MEMORY, totalProcessSharedMemory);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_CPU_TOTAL_VIRTUAL_MEMORY, totalProcessVirtualMemory);

		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_KERNEL_SCHEDULING_PRIORITY, kernelSchedulingPriority);
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_ACTIVE_THREADS, numOfActiveThreads);

		// monitorMap.put(KEY_ARCH, MonitorData.safeS(arch));
		monitorMap.put(CloudifyConstants.USM_METRIC_AVAILABLE_PROCESSORS, availableProcessors);
		monitorMap.put(CloudifyConstants.USM_METRIC_COMMITTED_VIRTUAL_MEM_SIZE, committedVirtualMemorySize);
		// monitorMap.put(KEY_OS_NAME, MonitorData.safeS(osName));
		monitorMap.put(CloudifyConstants.USM_METRIC_PROCESS_CPU_TIME, processCpuTime);
		// map.put("Classpath", safeS(classPath));
		monitorMap.put(CloudifyConstants.USM_METRIC_THREAD_COUNT, threadCount);
		monitorMap.put(CloudifyConstants.USM_METRIC_PEAK_THREAD_COUNT, peakThreadCount);
	}


	private double processCpuUsage;
	private long processCpuKernelTime;
	private long totalProcessCpuTime;

	private long processGroupId;
	private long processUserId;





	private long totalNumOfPageFaults;
	private long totalProcessResidentalMemory;
	private long totalProcessSharedMemory;
	private long totalProcessVirtualMemory;


	private int kernelSchedulingPriority;
	private long numOfActiveThreads;

	private int availableProcessors; 
	private long committedVirtualMemorySize; 
	
	private long processCpuTime; 
	private int threadCount; 
	private int peakThreadCount; 
}
