/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.usm.locator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.PreStartListener;
import org.cloudifysource.usm.events.StartReason;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.gigaspaces.internal.sigar.SigarHolder;

/************
 * A process locator implementation that is executed if no other process locator is defined. It scans the process tree
 * under the current process, comparing the processes running before the start command was called, and after start
 * detection passed successfully. Then it select the 'leaf' nodes of this tree. This gives us the 'interesting'
 * processes, assuming the service process runs in the foreground.
 *
 * This implementation is a heuristic, and works best when executing a single process in the foreground, typical for
 * multi-threaded processes like java application servers. It is generally a good idea for a process to explicitly
 * define its process locator, so that this locator is not used.
 *
 *
 * @author barakme
 *
 */
public class DefaultProcessLocator extends AbstractUSMEventListener implements ProcessLocator, PreStartListener {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(DefaultProcessLocator.class.getName());

	// process names for well-known shell
	// used to check if the monitored process is a shell, and not an
	// 'interesting' process
	private static final String[] SHELL_PROCESS_NAMES = { "cmd.exe", "bash", "/bin/sh" };

	private Sigar sigar;
	private long myPid;

	private Set<Long> childrenBeforeStart;

	private long childProcessID;
	private List<Long> serviceProcesses;

	@Override
	public List<Long> getProcessIDs()
			throws USMException {
		this.findProcessIDs();
		return this.serviceProcesses;
	}

	@Override
	public EventResult onPreStart(final StartReason reason) {
		this.myPid = this.sigar.getPid();

		try {
			this.childrenBeforeStart = getChildProcesses(this.myPid);
		} catch (final USMException e) {
			throw new IllegalStateException("Failed to read child processes", e);
		}

		return EventResult.SUCCESS;

	}

	/**********
	 * Creates a process tree of the given Process IDs. Each entry in the map maps a PID to the set of its child PIDs.
	 *
	 * @param pids
	 * @return
	 */
	private Map<Long, Set<Long>> createProcessTree(final long[] pids) {

		final HashMap<Long, Set<Long>> map = new HashMap<Long, Set<Long>>();
		for (final long pid : pids) {
			final Set<Long> childSet = map.get(pid);
			if (childSet == null) {
				map.put(pid, new HashSet<Long>());
			}

			try {
				final long ppid = this.sigar.getProcState(pid).getPpid();

				Set<Long> set = map.get(ppid);
				if (set == null) {
					set = new HashSet<Long>();
					map.put(ppid, set);
				}
				set.add(pid);
			} catch (final SigarException e) {
				logger.log(Level.WARNING, "Failed to get Parent Process for process: " + pid, e);
			}
		}

		return map;

	}

	private long findNewChildProcessID(final Set<Long> childrenBefore, final Map<Long, Set<Long>> procTree)
			throws USMException {
		final Set<Long> childrenAfter = procTree.get(this.myPid);
		if (childrenAfter == null) {
			throw new USMException("Could not find container process (" + this.myPid + ") in generated process tree");
		}
		childrenAfter.removeAll(childrenBefore);

		if (childrenAfter.isEmpty()) {
			logger.warning("Default process locator could not find a new process! "
					+ "Are you running your service as a background process or a system service?");
			return 0;

		} else if (childrenAfter.size() > 1) {
			logger.warning("Multiple new processes have been found: " + childrenAfter.toString()
					+ ". Using the first as child process ID!");
		}

		final long newChildProcessID = childrenAfter.iterator().next();

		return newChildProcessID;
	}

	/**********
	 * Recursive function that scans the given process tree, rooted at the given parent PID, and add all leaf pids to
	 * the result list.
	 *
	 * @param parentProcessID
	 *            the root of the tree.
	 * @param procTree
	 *            the full process tree.
	 * @param leafPids
	 *            the result leaf pids list.
	 */
	private void findLeafProcessIDs(final long parentProcessID, final Map<Long, Set<Long>> procTree,
			final List<Long> leafPids) {

		final Set<Long> pids = procTree.get(parentProcessID);

		if (pids == null || pids.isEmpty()) {
			leafPids.add(parentProcessID);
			return;
		}

		for (final Long pid : pids) {
			// Recursive call
			findLeafProcessIDs(pid, procTree, leafPids);

		}

	}

	private void findProcessIDs()
			throws USMException {

		final long[] allPids = getAllPids();
		final Map<Long, Set<Long>> procTree = createProcessTree(allPids);
		this.childProcessID = findNewChildProcessID(childrenBeforeStart, procTree);
		if (this.childProcessID == 0) {
			logger.warning("Default foreground process locator was unable to locate a new child process. "
					+ "The default implementation can only locate foreground processes. "
					+ "If you are running backgorund processes or OS services, you must "
					+ "set a process locator to get process level metrics and monitoring");
			this.serviceProcesses = new ArrayList<Long>(0);
		} else {

			logger.info("Looking for actual process ID in process tree");
			final List<Long> resultList = new LinkedList<Long>();
			findLeafProcessIDs(this.childProcessID, procTree, resultList);

			if (resultList.isEmpty()) {
				logger.warning("Default process locator was unable to locate service processes. "
						+ "The default implementation can only locate foreground processes. "
						+ "If you are running backgorund processes or OS services, you must "
						+ "set a process locator to get process level metrics and monitoring");
			}

			this.serviceProcesses = resultList;
			// also logs process details to logger.
			checkForConsoleProcess();
		}

	}

	private void checkForConsoleProcess() {
		final List<Long> pids = this.serviceProcesses;
		for (final Long pid : pids) {
			try {
				String procName = this.sigar.getProcExe(pid).getName();
				String[] procArgs = this.sigar.getProcArgs(pid);

				// sigar could return anything...
				if (procName == null) {
					procName = "Unknown";
				}
				if (procArgs == null) {
					procArgs = new String[0];
				}
				logger.info("Located process (" + pid + "): " + procName + " " + Arrays.toString(procArgs));
				for (final String shellName : SHELL_PROCESS_NAMES) {
					if (procName.contains(shellName)) {
						logger.warning("A monitored process(" + pid + " - " + procName + ") may be a console process. "
								+ "This is usually a configuration problem. "
								+ "USM Statistics will be collected for this process, "
								+ "and not for the child process it probably has. Are you missing a Start Detector?");
					}
				}
			} catch (final SigarException e) {
				logger.log(Level.SEVERE,
						"While checking if process is a console, failed to read the process name for process: " + pid,
						e);
			}

		}

	}

	private Set<Long> getChildProcesses(final long ppid)
			throws USMException {
		final long[] pids = getAllPids();
		return getChildProcesses(ppid, pids);
	}

	private long[] getAllPids()
			throws USMException {
		long[] pids;
		try {
			pids = this.sigar.getProcList();
		} catch (final SigarException se) {
			throw new USMException("Failed to look up process IDs. Error was: " + se.getMessage(), se);
		}
		return pids;
	}

	private Set<Long> getChildProcesses(final long ppid, final long[] pids) {

		final Set<Long> children = new HashSet<Long>();
		for (final long pid : pids) {
			try {
				if (ppid == this.sigar.getProcState(pid).getPpid()) {
					children.add(pid);
				}
			} catch (final SigarException e) {
				logger.log(Level.WARNING, "While scanning for child processes of process " + ppid
						+ ", could not read process state of Process: " + pid + ". Ignoring.", e);

			}

		}
		return children;

	}

	@Override
	public void init(final UniversalServiceManagerBean usm) {
		super.init(usm);

		this.sigar = SigarHolder.getSigar();
	}

}
