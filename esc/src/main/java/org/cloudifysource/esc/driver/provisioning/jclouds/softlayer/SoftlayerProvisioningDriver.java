/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.esc.driver.provisioning.jclouds.softlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.softlayer.compute.functions.guest.VirtualGuestToNodeMetadata;
import org.jclouds.softlayer.compute.functions.guest.VirtualGuestToReducedNodeMetaData;
import org.jclouds.softlayer.reference.SoftLayerConstants;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

/**
 * This driver injects a custom module to jclouds in order to boost poor performance on softlayer.
 *
 * @author Eli Polonsky
 * @since 2.7.0
 */

public class SoftlayerProvisioningDriver extends DefaultProvisioningDriver {
	
	private static final String CLEANUP_SCRIPT_ON_MACHINE_FAILURE = "cleanupScriptOnMachineFailure";
	private static final String FAILED_MACHINE_PRIVATE_ADDRESS = "FAILED_MACHINE_PRIVATE_ADDRESS";
	private static final String FAILED_MACHINE_PUBLIC_ADDRESS = "FAILED_MACHINE_PUBLIC_ADDRESS";
	private static final String FAILED_MACHINE_ID = "FAILED_MACHINE_ID";
	private static final String FAILED_MACHINE_LOCATION_ID = "FAILED_MACHINE_LOCATION_ID";
	private static final String FAILED_MACHINE_ENV_VARS = "FAILED_MACHINE_ENV_VARS";

    @Override
    public Set<Module> setupModules(final String templateName, final ComputeTemplate template) {
        Set<Module> modules = super.setupModules(templateName, template);
        int packageId = Utils.getInteger(template.getOverrides()
                .get(SoftLayerConstants.PROPERTY_SOFTLAYER_PACKAGE_ID), 46);
        if (packageId == 46) { // We are using virtual guests
            modules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VirtualGuestToNodeMetadata.class).to(VirtualGuestToReducedNodeMetaData.class);
                }
            });
        }
        return modules;
    }
    
    @Override
	public void onMachineFailure(final ProvisioningContext context, final long duration, final TimeUnit unit) 
			throws CloudProvisioningException, TimeoutException {
		
		logger.finest("Handling compute resources following machine failure");
		
		// customary call to the super implementation
		super.onMachineFailure(context, duration, unit);
		
		runCleanupScriptOnMachineFailure(context.getPreviousMachineDetails());
	}
    
    
    private void runCleanupScriptOnMachineFailure(final MachineDetails failedMachineDetails) 
			throws CloudProvisioningException {
    	
    	String cleanupScript = getCleanupScriptValue();
    	File scriptFile = new File(cleanupScript);
		if (!scriptFile.isFile()) {
			String errMsg = "The cleanup script file denoted by \"" + cleanupScript 
					+ "\" does not exist or is not a file";
			logger.warning(errMsg);
			throw new CloudProvisioningException(errMsg);
		}
		
		// run the script and pass the machine details of the failed machine as env vars
		logger.info("Executing cleanup script following failure of machine: " 
				+ failedMachineDetails.toString());
		Map<String, String> commandEnvVars = machineDetailsToEnvVars(failedMachineDetails);
		try {
			runScriptInSubProcess(scriptFile.getAbsolutePath(), commandEnvVars);							
		} catch (Exception e) {
			String errMsg = "An error encountered during the execution of cleanup script \"" 
					+ cleanupScript + "\", " + e.getMessage();
			throw new CloudProvisioningException(errMsg, e);
		}
	}
    
    
    private String getCleanupScriptValue() {
    	
    	String cleanupScriptValue = "";
    	
    	final Map<String, Object> customSettings = cloud.getCustom();
		if (customSettings != null) {
			// get cleanup script if set
			if (customSettings.containsKey(CLEANUP_SCRIPT_ON_MACHINE_FAILURE)) {
				final Object cleanupScriptObj = customSettings.get(CLEANUP_SCRIPT_ON_MACHINE_FAILURE);
				if (cleanupScriptObj != null) {
					if (cleanupScriptObj instanceof String) {
						cleanupScriptValue = (String) cleanupScriptObj;
	                } else {
	                    throw new IllegalArgumentException("Unexpected value for Softlayer cloud driver property: "
	                            + CLEANUP_SCRIPT_ON_MACHINE_FAILURE + ". A String value was expected, but got: "
	                            + cleanupScriptValue.getClass().getName());
	                }
				}
			}
		}
		
		return cleanupScriptValue;
    }
    
    
    private Map<String, String> machineDetailsToEnvVars(final MachineDetails failedMachineDetails) {
    	Map<String, String> envVars = new HashMap<String, String>();
    	envVars.put(FAILED_MACHINE_PRIVATE_ADDRESS, failedMachineDetails.getPrivateAddress());
    	envVars.put(FAILED_MACHINE_PUBLIC_ADDRESS, failedMachineDetails.getPublicAddress());
    	envVars.put(FAILED_MACHINE_ID, failedMachineDetails.getMachineId());
    	envVars.put(FAILED_MACHINE_LOCATION_ID, failedMachineDetails.getLocationId());
    	envVars.put(FAILED_MACHINE_ENV_VARS, 
    			Arrays.toString(failedMachineDetails.getEnvironment().entrySet().toArray()));
    	
    	return envVars;
    }
    
    
    private void runScriptInSubProcess(final String command, final Map<String, String> envVars) throws IOException {
    	String cmdLine = command;
    	
    	if (isWindows()) {
    		// need to use the call command to intercept the cloudify batch file return code.
    		cmdLine = "cmd /c call " + cmdLine;
    	}
    	
    	final String[] parts = cmdLine.split(" ");
    	final ProcessBuilder pb = new ProcessBuilder(parts);
    	pb.redirectErrorStream(true);

        // apply additional environment variables if set
        for (Entry<String, String> envVar : envVars.entrySet()) {
        	if (envVar.getValue() == null) {
        		// setting null env vars as "" to avoid a NPE
        		logger.fine("script environment variable " + envVar.getKey() + ": \"\"");
        		pb.environment().put(envVar.getKey(), "");
        	} else {
        		logger.fine("script environment variable " + envVar.getKey() + ": \"" + envVar.getValue() + "\"");
        		pb.environment().put(envVar.getKey(), envVar.getValue());        		
        	}
        }
        
    	logger.info("command " + command + " will be executed with env: \"" + pb.environment() + "\"");
    	
    	final Process process = pb.start();
    	
    	// handle process results
    	ProcessResult processResult = getProcessResult(process);
		if (processResult.getExitcode() == 0) {
			logger.info("script output: " + processResult.getOutput());
		} else {
			logger.warning("Execution of cleanup script " + command + "did not end succesfully, exit code was " 
				+ processResult.getExitcode() + "output is " + processResult.getOutput());
		}
    }
    
    
    private ProcessResult getProcessResult(final Process process) {
		// Print CLI output if exists.
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		final StringBuilder consoleOutput = new StringBuilder("");
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
		int exitcode = -1;
		
		try {
			Thread thread = new Thread(new Runnable() {
				String line = null;
				@Override
				public void run() {	
					try {
						while ((line = br.readLine()) != null) {
							consoleOutput.append(line + "\n");
						}
					} catch (Throwable e) {
						exception.set(e);
					}
					
				}
			});
			
			thread.setDaemon(true);
			thread.start();
			exitcode = process.waitFor();
			thread.join(5000);
		} catch (InterruptedException e) {
			logger.warning("Failed to get process output. output = " + consoleOutput 
					+ ", reported error: " + exception.get().getMessage());
		}
		
		if (exception.get() != null) {
			logger.warning("Failed to get process output. output = " + consoleOutput 
					+ ", reported error: " + exception.get().getMessage());
		}
		
		String stdout = consoleOutput.toString();
		return new ProcessResult(stdout, exitcode);
	}
    
    
    protected class ProcessResult {
    	
    	private final String output;
    	private final int exitcode;
    	
    	public ProcessResult(final String output, final int exitcode) {
			this.output = output;
			this.exitcode = exitcode;
		}
    	
		@Override
		public String toString() {
			return "ProcessResult [output=" + getOutput() + ", exitcode=" + getExitcode()
					+ "]";
		}

		public String getOutput() {
			return output;
		}

		public int getExitcode() {
			return exitcode;
		}
    }
    
    
    public static boolean isWindows() {
    	boolean isWindows = false;
    	String osNameProp = System.getProperty("os.name");
    	if (StringUtils.isNotBlank(osNameProp)) {
    		isWindows = osNameProp.toLowerCase().startsWith("win");
    	}
    	
		return isWindows;
	}
    
}
