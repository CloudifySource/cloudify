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
package org.cloudifysource.azure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class AzureConfigExe {

	protected static final String CREATE_HOSTED_SERVICE_FLAG = "create-hosted-service";
	protected static final String LOCATION_FLAG = "location";
	protected static final String CREATE_DEPLOYMENT_FLAG = "create-deployment";
	protected static final String UPDATE_DEPLOYMENT_FLAG = "update-deployment";
	protected static final String DELETE_DEPLOYMENT_FLAG = "delete-deployment";
	protected static final String GET_DEPLOYMENT_STATUS_FLAG = "get-deployment-status";
	protected static final String GET_DEPLOYMENT_URL_FLAG = "get-deployment-url";
	protected static final String GET_DEPLOYMENT_CONFIG_FLAG = "get-deployment-config";
	protected static final String SET_DEPLOYMENT_CONFIG_FLAG = "set-deployment-config";
	protected static final String LIST_HOSTED_SERVICES_FLAG = "list-hosted-services";
	protected static final String LIST_LOCATIONS_FLAG = "list-locations";
	protected static final String LIST_CERTIFICATES_FLAG = "list-certificates";
	protected static final String SLOT_FLAG = "slot";
	protected static final String SUBSCRIPTION_ID_FLAG = "subscription-id";
	protected static final String CERTIFICATE_THUMBPRINT_FLAG = "certificate-thumbprint";
	protected static final String HOSTED_SERVICE_FLAG = "hosted-service";
	protected static final String NAME_FLAG = "name";
	protected static final String LABEL_FLAG = "label";
	protected static final String CONFIG_FLAG = "config";
	protected static final String PACKAGE_FLAG = "package";
	protected static final String STATUS_FLAG = "status";
	protected static final String ADD_CERTIFICATE_FLAG = "add-certificate";
	protected static final String CERTIFICATE_FILE_PASSWORD_FLAG = "cert-file-password";
	protected static final String CERTIFICATE_FILE_FLAG = "cert-file";
	protected static final String DESCRIPTION_FLAG = "description";
	
	protected static final String STORAGE_KEY_FLAG = "storage-key";
	protected static final String STORAGE_ACCOUNT_FLAG = "storage-account";
	protected static final String STORAGE_CONTAINER_FLAG = "storage-container";
	
	private final File azureConfigExeFile;
	private final File encUtilExeFile;
	private final String subscriptionId;
	private final String certificateThumbprint;
	private final boolean verbose;
	/**
	 * 
	 * @param azureConfigExeFile - path to azureconfig.exe that implements the azure API in C#
	 * @param verbose - true prints full exception stack trace, false prints only the message
	 */
	AzureConfigExe(File azureConfigExeFile, File encUtilExeFile, String subscriptionId, String certificateThumbprint, boolean verbose) {
		this.azureConfigExeFile = azureConfigExeFile;
		this.encUtilExeFile = encUtilExeFile;
		this.subscriptionId = subscriptionId;
		this.certificateThumbprint = certificateThumbprint;
		this.verbose = verbose;
	}

	protected String executeAzureConfig(final String... args)
		throws InterruptedException, AzureDeploymentException {
	    
        String[] newArgs = Arrays.copyOf(args, args.length+2);
        newArgs[args.length] = argument(CERTIFICATE_THUMBPRINT_FLAG, certificateThumbprint);
        newArgs[args.length+1] = argument(SUBSCRIPTION_ID_FLAG, subscriptionId);
        
        return execute(azureConfigExeFile.getAbsolutePath(),verbose,newArgs);
	}

	protected String executeEncUtil(String... args) throws AzureDeploymentException, InterruptedException {
		return execute(encUtilExeFile.getAbsolutePath(),false,args);
	}
	
	protected String execute(String exeFile, boolean verbose, final String... args) throws AzureDeploymentException, InterruptedException {

		String[] cmd = new String[args.length + (verbose?2:1)];
		int targetIndex = 0;
		// file path
		cmd[targetIndex] = exeFile;
		targetIndex++;
		// requested command
		cmd[targetIndex] = args[0];
		targetIndex++;
		// optional verbose flag
		if (verbose) {
			// the /verbose instructs azureconfig.exe to print full exception stack trace
			cmd[targetIndex] = argument("verbose");
			targetIndex++;
		}
		
		// other options
		System.arraycopy(args, 1, cmd, targetIndex, args.length-1);
		
		final ProcessBuilder pb = new ProcessBuilder(cmd);

		pb.redirectErrorStream(true);

		if (verbose) {
			System.out.println("Executing command: " + Arrays.toString(cmd));
		}
		Process p;
		try {
			p = pb.start();
		} catch (IOException e) {
			throw new AzureDeploymentException(
					"error occured while running " + Arrays.toString(cmd), e);
		}

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		final StringBuilder sb = new StringBuilder();
		try {
			String line = reader.readLine();
			while (line != null) {
				sb.append(line).append('\n');
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new AzureDeploymentException(
					"error occured while running " + Arrays.toString(cmd), e);
		}

		final String readResult = sb.toString();
		// we are using waitFor() instead of exitCode() since output redirection may not always work
		// for example when the command involves the redirection ">" operator
		final int exitValue = p.waitFor();
		if (exitValue != 0) {
			throw new AzureDeploymentException(
					exeFile + " exit code: " + exitValue
							+ "\n Process output:\n" + readResult);
		}

		return readResult;
		
	}
	

	protected static String argument(String flag, String value) {
		return '/' + flag + ':' + value;
	}

	protected static String argument(String flag) {
		return '/' + flag;
	}
}