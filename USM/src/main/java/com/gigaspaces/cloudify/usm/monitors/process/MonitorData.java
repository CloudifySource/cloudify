package com.gigaspaces.cloudify.usm.monitors.process;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.gigaspaces.cloudify.usm.monitors.MonitorException;

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

	private void gatherData(final Sigar sigar, final long pid) throws SigarException {

		try {
			final ProcCpu pcpu = sigar.getProcCpu(pid);
			processCpuUsage = pcpu.getPercent();
			processCpuKernelTime = pcpu.getSys();
			totalProcessCpuTime = pcpu.getTotal(); // sum of users+sys
		} catch (final SigarException e) {
			logger.log(Level.FINE, "Failed to gather process info from Sigar: " + e.getMessage(), e);
		}

		try {
			processArguments = sigar.getProcArgs(pid);
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
			final ProcCredName prcredname = sigar.getProcCredName(pid);
			processOwnerGroupName = prcredname.getGroup();
			processOwnerUserName = prcredname.getUser();
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

		//monitorMap.put("Process Owner Group Name", MonitorData.safeS(processOwnerGroupName));
		// monitorMap.put("Process Owner User Name", MonitorData.safeS(processOwnerUserName));

		monitorMap.put("Total Num Of PageFaults", totalNumOfPageFaults);
		monitorMap.put("Total Process Residental Memory", totalProcessResidentalMemory);
		monitorMap.put("Total Process Shared Memory", totalProcessSharedMemory);
		monitorMap.put("Total Process Virtual Memory", totalProcessVirtualMemory);

		monitorMap.put("Kernel Scheduling Priority", kernelSchedulingPriority);
		monitorMap.put("Num Of Active Threads", numOfActiveThreads);

		//monitorMap.put(KEY_ARCH, MonitorData.safeS(arch));
		monitorMap.put(KEY_AVAIL_PROCESSORS, availableProcessors);
		monitorMap.put(KEY_COMMIT_VIRT_MEM_SIZE, committedVirtualMemorySize);
		//monitorMap.put(KEY_OS_NAME, MonitorData.safeS(osName));
		monitorMap.put(KEY_PROC_CPU_TIME, processCpuTime);
		// map.put("Classpath", safeS(classPath));
		monitorMap.put(KEY_THREAD_COUNT, threadCount);
		monitorMap.put(KEY_PEAK_THREAD_COUNT, peakThreadCount);
	}

	private static String flattenStrArr(final String[] arr) {
		if (arr == null) {
			return "";
		}
		final StringBuilder res = new StringBuilder();
		for (final String str : arr) {
			res.append(' ').append(str);
		}
		return res.toString();
	}

	private static String safeS(final String v) {
		if (v == null) {
			return "";
		}
		if (v.equals("null")) {
			return "";
		}
		return v;
	}

	// ProcCpu pcpu = sigar.getProcCpu(pid);
	private double processCpuUsage;// = pcpu.getPercent();
	private long processCpuKernelTime;// = pcpu.getSys();
	private long totalProcessCpuTime;// = pcpu.getTotal(); // sum of users+sys

	private String[] processArguments;// = sigar.getProcArgs(pid) ;

	// ProcCred prcred = sigar.getProcCred(pid);
	private long processGroupId;// = prcred.getGid();
	private long processUserId;// = prcred.getUid();

	// ProcCredName prcredname = sigar.getProcCredName(pid);
	private String processOwnerGroupName;// = prcredname.getGroup();
	private String processOwnerUserName;// = prcredname.getUser();

	// ProcMem pmem = sigar.getProcMem(pid);
	private long totalNumOfPageFaults;// = pmem.getPageFaults();
	private long totalProcessResidentalMemory;// = pmem.getResident();
	private long totalProcessSharedMemory;// = pmem.getShare();
	private long totalProcessVirtualMemory;// = pmem.getSize();

	// ProcState prcstat = sigar.getProcState(pid);
	private int kernelSchedulingPriority;// = prcstat.getPriority();
	private long numOfActiveThreads;// = prcstat.getThreads();

	// from jmx
	private String arch; // = (String) resMap.get("Arch"); // s="x86"
	private int availableProcessors; // =
										// (Integer)resMap.get("AvailableProcessors");
	private long committedVirtualMemorySize; // =
												// (Long)resMap.get("CommittedVirtualMemorySize");
	private String osName; // = (String) resMap.get("Name"); // s="Windows XP"
	private long processCpuTime; // = (Long)resMap.get("ProcessCpuTime");
	// private String classPath; // = (String) resMap.get("ClassPath");
	private int threadCount; // = (Integer) resMap.get("ThreadCount");
	private int peakThreadCount; // = (Integer) resMap.get("PeakThreadCount");

	private static final String KEY_ARCH = "Arch";
	private static final String KEY_AVAIL_PROCESSORS = "Available Processors";
	private static final String KEY_COMMIT_VIRT_MEM_SIZE = "Committed Virtual Memory Size";
	private static final String KEY_OS_NAME = "OS Name"; // s="Windows XP"
	private static final String KEY_PROC_CPU_TIME = "Process Cpu Time";
	// private static final String KEY_CLASSPATH = "ClassPath";
	private static final String KEY_THREAD_COUNT = "Thread Count";
	private static final String KEY_PEAK_THREAD_COUNT = "Peak Thread Count";

}
