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

import org.cloudifysource.usm.monitors.MonitorException;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;

import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/**
 * Adds monitor targets to be polled by the JMX monitor thread
 * 
 * @author giladh, barakme
 * @since 8.0.2
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

	public Map<String, Number> getDataMap() {
		final Map<String, Number> map = new HashMap<String, Number>();
		addDataToMonitor(map);
		return map;
	}

	public void addDataToMonitor(final Map<String, Number> monitorMap) {
		monitorMap.put("Process Cpu Usage", processCpuUsage);
		monitorMap.put("Process Cpu Kernel Time", processCpuKernelTime);
		monitorMap.put("Total Process Cpu Time", totalProcessCpuTime);

		// monitorMap.put("Process Arguments", MonitorData.flattenStrArr(processArguments));

		monitorMap.put("Process GroupId", processGroupId);
		monitorMap.put("Process User Id", processUserId);

		// monitorMap.put("Process Owner Group Name", MonitorData.safeS(processOwnerGroupName));
		// monitorMap.put("Process Owner User Name", MonitorData.safeS(processOwnerUserName));

		monitorMap.put("Total Num Of PageFaults", totalNumOfPageFaults);
		monitorMap.put("Total Process Residental Memory", totalProcessResidentalMemory);
		monitorMap.put("Total Process Shared Memory", totalProcessSharedMemory);
		monitorMap.put("Total Process Virtual Memory", totalProcessVirtualMemory);

		monitorMap.put("Kernel Scheduling Priority", kernelSchedulingPriority);
		monitorMap.put("Num Of Active Threads", numOfActiveThreads);

		// monitorMap.put(KEY_ARCH, MonitorData.safeS(arch));
		monitorMap.put(KEY_AVAIL_PROCESSORS, availableProcessors);
		monitorMap.put(KEY_COMMIT_VIRT_MEM_SIZE, committedVirtualMemorySize);
		// monitorMap.put(KEY_OS_NAME, MonitorData.safeS(osName));
		monitorMap.put(KEY_PROC_CPU_TIME, processCpuTime);
		// map.put("Classpath", safeS(classPath));
		monitorMap.put(KEY_THREAD_COUNT, threadCount);
		monitorMap.put(KEY_PEAK_THREAD_COUNT, peakThreadCount);
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


	private static final String KEY_AVAIL_PROCESSORS = "Available Processors";
	private static final String KEY_COMMIT_VIRT_MEM_SIZE = "Committed Virtual Memory Size";
	private static final String KEY_PROC_CPU_TIME = "Process Cpu Time";
	private static final String KEY_THREAD_COUNT = "Thread Count";
	private static final String KEY_PEAK_THREAD_COUNT = "Peak Thread Count";

}
