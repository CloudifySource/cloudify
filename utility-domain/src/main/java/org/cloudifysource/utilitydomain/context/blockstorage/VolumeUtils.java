/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.utilitydomain.context.blockstorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.StorageFacade;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.gigaspaces.internal.sigar.SigarHolder;


/**
 * 
 * @author elip
 *
 */
public final class VolumeUtils {
	
	// prevent instantiation.
	private VolumeUtils() {
		
	}
	
	private static final Logger logger = Logger.getLogger(VolumeUtils.class.getName());

	private static final String USER_NAME = System.getProperty("user.name");	
		
	private static final long MOUNT_TIMEOUT = 30 * 1000;
	private static final long FORMAT_TIMEOUT = 5 * 60 * 1000;
	private static final long PARTITION_TIMEOUT = 30 * 1000;
	private static final long UNMOUNT_TIMEOUT = 15 * 1000;
	
	private static final long TEN_SECONDS = 10 * 1000;
	
	/**
	 * @see {@link StorageFacade#unmount(String, long)}.
	 * @param device .
	 * @param timeoutInMillis .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void unmount(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		executeCommandLine("sudo umount -d -v -l -f " + device, timeoutInMillis);
	}

	/**
	 * @see {@link StorageFacade#unmount(String)}
	 * @param device . 
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void unmount(final String device) throws LocalStorageOperationException, TimeoutException {
		unmount(device, UNMOUNT_TIMEOUT);
	}

	/**
	 * @see {@link StorageFacade#mount(String, String, long)}
	 * @param device .
	 * @param path .
	 * @param timeoutInMillis .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void mount(final String device, final String path, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		File devicePath = new File(path);
        executeCommandLine("sudo mkdir " + path, TEN_SECONDS);
        executeCommandLine("sudo mount " + device + " " + path, timeoutInMillis);
		executeCommandLine("sudo chown " + USER_NAME + " " + devicePath.getAbsolutePath(), TEN_SECONDS);
	}

	/**
	 * @see {@link StorageFacade#mount(String, String)}
	 * @param device .
	 * @param path .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void mount(final String device, final String path) 
			throws LocalStorageOperationException, TimeoutException {
		mount(device, path, MOUNT_TIMEOUT);
	}

	/**
	 * @see {@link StorageFacade#format(String, String, long)}
	 * @param device . 
	 * @param fileSystem .
	 * @param timeoutInMillis .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void format(final String device, final String fileSystem, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		checkFileSystemSupported(fileSystem);
		executeCommandLine("sudo mkfs -t " + fileSystem + " " + device, timeoutInMillis);
		
	}

	/**
	 * @see {@link StorageFacade#format(String, String)}.
	 * @param device .
	 * @param fileSystem .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void format(final String device, final String fileSystem) 
			throws LocalStorageOperationException, TimeoutException {
		format(device, fileSystem, FORMAT_TIMEOUT);
	}
	
	/**
	 * @see {@link StorageFacade#partition(String, long)}
	 * @param device .
	 * @param timeoutInMillis .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void partition(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		try {
			File tempParitioningScript = File.createTempFile("partitionvolume", ".sh");
			FileUtils.writeStringToFile(tempParitioningScript, 
					"(echo o; echo n; echo p; echo 1; echo; echo; echo w) | sudo fdisk " + device);
			tempParitioningScript.setExecutable(true);
			tempParitioningScript.deleteOnExit();
			executeCommandLine(tempParitioningScript.getAbsolutePath(), timeoutInMillis);
		} catch (IOException ioe) {
			// fdisk returns exit code 1 even when successful so we have to verify
			logger.info("inspecting fdisk command exception: " + ioe.getMessage());
			if (!verifyDevicePartitioning(device, timeoutInMillis)) {
				throw new LocalStorageOperationException("Failed to partition device " + device + ", reported error: "
						+ ioe.getMessage(), ioe);
			}
		}

	}
	
	private static boolean verifyDevicePartitioning(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		
		boolean deviceFormatted = false;

		try {
			File tempDeviceListScript = File.createTempFile("getPartitionStatus", ".sh");
			FileUtils.writeStringToFile(tempDeviceListScript, "sudo fdisk -l | grep " + device);
			tempDeviceListScript.setExecutable(true);
			tempDeviceListScript.deleteOnExit();
			String fdiskOutput = executeSilentCommandLineReturnOutput(tempDeviceListScript.getAbsolutePath(), timeoutInMillis);
			if (fdiskOutput.contains(device + ":") && 
					!fdiskOutput.contains(device + " doesn't contain a valid partition table")) {
				deviceFormatted = true;
			}
		} catch (Exception e) {
			throw new LocalStorageOperationException("Failed verifying paritioning of device: " + device, e);
		}
		
		return deviceFormatted;
		
	}

	/**
	 * @see {@link StorageFacade#partition(String)}.
	 * @param device .
	 * @throws LocalStorageOperationException .
	 * @throws TimeoutException .
	 */
	public static void partition(final String device) 
			throws LocalStorageOperationException, TimeoutException {
		partition(device, PARTITION_TIMEOUT);
	}

	private static void checkFileSystemSupported(final String fileSystem) throws LocalStorageOperationException {

		FileSystem[] fileSystemList;
		try {
			Sigar sigar = SigarHolder.getSigar();
			fileSystemList = sigar.getFileSystemList();
		} catch (final SigarException e) {
			throw new LocalStorageOperationException(e);
		}
		
		List<String> supportedFileSystems = new ArrayList<String>();
		for (FileSystem fs : fileSystemList) {
			if (fileSystem.equalsIgnoreCase(fs.getSysTypeName())) {
				return;
			} else {
				supportedFileSystems.add(fs.getSysTypeName());
			}
		}
		throw new LocalStorageOperationException("File system type " + fileSystem 
					+ " is not supported for the current operating system. Supported File Systems are : " 
				+ StringUtils.join(supportedFileSystems, ","));		
	}
	
	private static void executeCommandLine(final String commandLine, final long timeout) 
			throws LocalStorageOperationException, TimeoutException {
		
		Executor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
		executor.setWatchdog(watchdog);
		ProcessOutputStream outAndErr = new ProcessOutputStream();
		try {
			PumpStreamHandler streamHandler = new PumpStreamHandler(outAndErr);
			executor.setStreamHandler(streamHandler);
			logger.info("Executing commandLine : '" + commandLine + "'");
			executor.execute(CommandLine.parse(commandLine));
			logger.info("Execution completed successfully. Process output was : " + outAndErr.getOutput());
		} catch (final Exception e) {
			if (watchdog.killedProcess()) {
				throw new TimeoutException("Timed out while executing commandLine : '" + commandLine + "'");
			}

			throw new LocalStorageOperationException("Failed executing commandLine : '" + commandLine 
					+ ". Process output was : " + outAndErr.getOutput(), e);
		}
	}
	
	private static String executeSilentCommandLineReturnOutput(final String commandLine, final long timeout) 
			throws LocalStorageOperationException, TimeoutException {
		
		Executor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
		executor.setWatchdog(watchdog);
		ProcessOutputStream outAndErr = new ProcessOutputStream();
		try {
			PumpStreamHandler streamHandler = new PumpStreamHandler(outAndErr);
			executor.setStreamHandler(streamHandler);
			executor.execute(CommandLine.parse(commandLine));
		} catch (final Exception e) {
			if (watchdog.killedProcess()) {
				throw new TimeoutException("Timed out while executing commandLine : '" + commandLine + "'");
			}

			throw new LocalStorageOperationException("Failed executing commandLine : '" + commandLine 
					+ ". Process output was : " + outAndErr.getOutput(), e);
		}
		
		return outAndErr.getOutput();
	}
	
	/**
	 * Logs process output to the logger.
	 * @author elip
	 *
	 */
	private static class ProcessOutputStream extends LogOutputStream {
		
		private List<String> output = new java.util.LinkedList<String>();
		
		private String getOutput() {
			return StringUtils.join(output, "\n");
		}
		
		@Override
		protected void processLine(final String line, final int level) {
			output.add(line);
			logger.info(line);
		}
	}
}
