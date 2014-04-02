/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cloudifysource.domain.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.RemoteExecutionModes;
import org.cloudifysource.domain.cloud.ScriptLanguages;

import com.gigaspaces.internal.io.IOUtils;

/*******
 * Described a Machine started by a cloud driver. MachineDetails implements @{link Externalizable} since it is embedded
 * in {@link org.cloudifysource.esc.driver.provisioning.events.MachineStartedCloudifyEvent}
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class MachineDetails implements Externalizable {

	static final long serialVersionUID = 5214124902481712415L;
	// TODO add version check to read/write external

	private String privateAddress;
	private String publicAddress;

	private boolean cloudifyInstalled = false;
	private String installationDirectory = null;
	private boolean agentRunning = false;

	// not serializable
	private transient String remoteUsername;
	// not serializable
	private transient String remotePassword;

	private String machineId;

	private FileTransferModes fileTransferMode = FileTransferModes.SFTP;
	private RemoteExecutionModes remoteExecutionMode = RemoteExecutionModes.SSH;
	private ScriptLanguages scriptLangeuage = ScriptLanguages.LINUX_SHELL;

	private String remoteDirectory;

	private String locationId;

	private boolean cleanRemoteDirectoryOnStart = false;

	// installer configuration. If null, default values should be used.
	private CloudTemplateInstallerConfiguration installerConfigutation = null;

	// it's rare, but clouds may return a key file as the password for a create server request.
	// In addition, a cloud driver may choose to generate a unique key file for each machine.
	private File keyFile;

	private String openFilesLimit;
	
	private String attachedVolumeId;

	private Map<String, String> environment = new LinkedHashMap<String, String>();

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(final String locationId) {
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
				+ publicAddress + ", gigaspacesInstalled=" + cloudifyInstalled + ", agentRunning=" + agentRunning
				+ ", installationDirectory=" + installationDirectory + ", locationId=" + locationId + "]";
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
	
	public String getAttachedVolumeId() {
		return attachedVolumeId;
	}

	public void setAttachedVolumeId(final String attachedVolumeId) {
		this.attachedVolumeId = attachedVolumeId;
	}

	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {

		privateAddress = IOUtils.readString(in);
		publicAddress = IOUtils.readString(in);
		cloudifyInstalled = in.readBoolean();
		installationDirectory = IOUtils.readString(in);
		agentRunning = in.readBoolean();
		// Do not pass username/password over the network! (PII)
		// remoteUsername = IOUtils.readString(in);
		// remotePassword = IOUtils.readString(in);
		machineId = IOUtils.readString(in);
		fileTransferMode = FileTransferModes.valueOf(IOUtils.readString(in));
		remoteExecutionMode = RemoteExecutionModes.valueOf(IOUtils.readString(in));
		this.scriptLangeuage = ScriptLanguages.valueOf(IOUtils.readString(in));
		remoteDirectory = IOUtils.readString(in);
		locationId = IOUtils.readString(in);
		openFilesLimit = IOUtils.readString(in);
		environment = IOUtils.readMapStringString(in);
	}

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {

		IOUtils.writeString(out, privateAddress);
		IOUtils.writeString(out, publicAddress);
		out.writeBoolean(cloudifyInstalled);
		IOUtils.writeString(out, installationDirectory);
		out.writeBoolean(agentRunning);
		// Do not pass username/password over the network! (PII)
		// IOUtils.writeString(out, remoteUsername);
		// IOUtils.writeString(out, remotePassword);
		IOUtils.writeString(out, machineId);
		IOUtils.writeString(out, fileTransferMode.name());
		IOUtils.writeString(out, remoteExecutionMode.name());
		IOUtils.writeString(out, scriptLangeuage.name());
		IOUtils.writeString(out, remoteDirectory);
		IOUtils.writeString(out, locationId);
		IOUtils.writeString(out, openFilesLimit);
		IOUtils.writeMapStringString(out, environment);
	}

	public ScriptLanguages getScriptLangeuage() {
		return scriptLangeuage;
	}

	public void setScriptLangeuage(final ScriptLanguages scriptLangeuage) {
		this.scriptLangeuage = scriptLangeuage;
	}

	public boolean isCleanRemoteDirectoryOnStart() {
		return cleanRemoteDirectoryOnStart;
	}

	public void setCleanRemoteDirectoryOnStart(final boolean cleanRemoteDirectoryOnStart) {
		this.cleanRemoteDirectoryOnStart = cleanRemoteDirectoryOnStart;
	}

	public CloudTemplateInstallerConfiguration getInstallerConfiguration() {
		return installerConfigutation;
	}

	public void setInstallerConfigutation(final CloudTemplateInstallerConfiguration installerConfigutation) {
		this.installerConfigutation = installerConfigutation;
	}

	public File getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(final File keyFile) {
		this.keyFile = keyFile;
	}

	public String getOpenFilesLimit() {
		return openFilesLimit;
	}

	public void setOpenFilesLimit(final String openFilesLimit) {
		this.openFilesLimit = openFilesLimit;
	}

	/*****
	 * Environment variables that should be made available on the machine, in addition to the standard ones that
	 * Cloudify uses. These variables will have priority over any previous ones defined by Cloudify or by the template.
	 * 
	 * @return the environment variables.
	 */
	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(final Map<String, String> environment) {
		this.environment = environment;
	}

}
