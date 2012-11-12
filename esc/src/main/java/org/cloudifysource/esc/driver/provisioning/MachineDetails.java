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
package org.cloudifysource.esc.driver.provisioning;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;

import com.gigaspaces.internal.io.IOUtils;

/*******
 * Described a Machine started by a cloud driver.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class MachineDetails implements Externalizable {
	
	private String privateAddress;
	private String publicAddress;
	
	private boolean cloudifyInstalled = false;
	private String installationDirectory = null;
	private boolean agentRunning = false;

	// not serializable
	private String remoteUsername;
	// not serializable
	private String remotePassword;

	private String machineId;

	private FileTransferModes fileTransferMode = FileTransferModes.SCP;
	private RemoteExecutionModes remoteExecutionMode = RemoteExecutionModes.SSH;

	private String remoteDirectory;
	
	private String locationId;
	
	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(final String machineId) {
		this.machineId = machineId;
	}

	public String getPrivateAddress() {
		return privateAddress;
	}

	public void setPrivateAddress(final String privateAddress) {
		this.privateAddress = privateAddress;
	}

	public String getPublicAddress() {
		return publicAddress;
	}

	public void setPublicAddress(final String publicAddress) {
		this.publicAddress = publicAddress;
	}

	public boolean isCloudifyInstalled() {
		return cloudifyInstalled;
	}

	public void setCloudifyInstalled(final boolean cloudifyInstalled) {
		this.cloudifyInstalled = cloudifyInstalled;
	}

	public boolean isAgentRunning() {
		return agentRunning;
	}

	public void setAgentRunning(final boolean agentRunning) {
		this.agentRunning = agentRunning;
	}

	public String getInstallationDirectory() {
		return installationDirectory;
	}

	public void setInstallationDirectory(final String installationDirectory) {
		this.installationDirectory = installationDirectory;
	}


	@Override
	public String toString() {
		return "MachineDetails [machineId=" + machineId + ", privateAddress=" + privateAddress + ", publicAddress="
				+ publicAddress + ", gigaspacesInstalled=" + cloudifyInstalled
				+ ", agentRunning=" + agentRunning + ", installationDirectory=" + installationDirectory + ", locationId=" + locationId + "]";
	}

	public String getRemoteUsername() {
		return remoteUsername;
	}

	public void setRemoteUsername(final String remoteUsername) {
		this.remoteUsername = remoteUsername;
	}

	public String getRemotePassword() {
		return remotePassword;
	}

	public void setRemotePassword(final String remotePassword) {
		this.remotePassword = remotePassword;
	}

	public FileTransferModes getFileTransferMode() {
		return fileTransferMode;
	}

	public void setFileTransferMode(final FileTransferModes fileTransferMode) {
		this.fileTransferMode = fileTransferMode;
	}

	public RemoteExecutionModes getRemoteExecutionMode() {
		return remoteExecutionMode;
	}

	public void setRemoteExecutionMode(final RemoteExecutionModes remoteExecutionMode) {
		this.remoteExecutionMode = remoteExecutionMode;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(final String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		privateAddress = IOUtils.readString(in);
		publicAddress = IOUtils.readString(in);
		cloudifyInstalled = in.readBoolean();
		installationDirectory = IOUtils.readString(in);
		agentRunning = in.readBoolean();
		//Do not pass username/password over the network! (PII)
		//remoteUsername = IOUtils.readString(in);
		//remotePassword = IOUtils.readString(in);
		machineId = IOUtils.readString(in);
		fileTransferMode = FileTransferModes.valueOf(IOUtils.readString(in));
		remoteExecutionMode = RemoteExecutionModes.valueOf(IOUtils.readString(in));
		remoteDirectory = IOUtils.readString(in);
		locationId = IOUtils.readString(in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		IOUtils.writeString(out, privateAddress);
		IOUtils.writeString(out, publicAddress);
		out.writeBoolean(cloudifyInstalled);
		IOUtils.writeString(out, installationDirectory);
		out.writeBoolean(agentRunning);
		//Do not pass username/password over the network! (PII)
		//IOUtils.writeString(out, remoteUsername);
		//IOUtils.writeString(out, remotePassword);
		IOUtils.writeString(out, machineId);
		IOUtils.writeString(out, fileTransferMode.name());
		IOUtils.writeString(out, remoteExecutionMode.name());
		IOUtils.writeString(out, remoteDirectory);
		IOUtils.writeString(out, locationId);
	}

}
