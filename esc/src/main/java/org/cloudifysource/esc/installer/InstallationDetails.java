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
package org.cloudifysource.esc.installer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GSAReservationId;

import com.gigaspaces.internal.utils.ReflectionUtils;

/************
 * Details for an installation request.
 * 
 * @author barakme
 * 
 */
public class InstallationDetails implements Cloneable {

	// IPs of the machine to install. ssh must already be running.
	private String publicIp;
	private String privateIp;

	private String zones = "";

	// ssh username
	private String username;

	// ssh password
	private String password;

	// ssh key file
	private String keyFile;

	// Locator that gigaspaces agent will use.
	private String locator;

	private String cloudifyUrl;
	private String overridesUrl;

	// An instance of the Gigaspaces Admin API. If passed,
	// will be used to check when an agent joins the cluster.
	private Admin admin;

	// true if this machine should act as the LUS and ESM
	private boolean isLus;

	// (only relevant in case isLus == true) if true no web-services will be deployed on the target machine
	private boolean noWebServices;

	// directory on local machine where installation files are
	// placed. At a minimum, the start-management.sh should be placed there.
	// Other files may include the gigaspaces installation, java,
	// and any other required scripts.
	private String localDir;

	// relative path to the local dir.
	private String relativeLocalDir;
	// The directory on the remote machine where installation
	// files will be uploaded to.
	private String remoteDir;

	// files that should be copied only to lus machines
	private String[] managementOnlyFiles;

	// wherther we are in the same network as the machine we are about to install
	private boolean connectedToPrivateIp;

	// whether the NIC_ADDR of the machine should be the private or public IP
	private boolean bindToPrivateIp = true;

	// a cloud specific identifier for a host
	private String machineId;

	private File cloudFile;

	private FileTransferModes fileTransferMode = FileTransferModes.SCP;
	private RemoteExecutionModes remoteExecutionMode = RemoteExecutionModes.SSH;

	private final Map<String, Object> customData = new HashMap<String, Object>();

	private Map<String, String> extraRemoteEnvironmentVariables = new HashMap<String, String>();

	// defines the com.gs.agent.reservationid system property for the GSA
	// see InternalGridServiceAgent#getReservationId()
	private GSAReservationId reservationId;
	
	public Admin getAdmin() {
		return admin;
	}

	public String getLocalDir() {
		return localDir;
	}

	public String getLocator() {
		return locator;
	}

	public String getPassword() {
		return password;
	}

	public String getRemoteDir() {
		return remoteDir;
	}

	public String getUsername() {
		return username;
	}

	// TODO - change to isManagement
	public boolean isLus() {
		return isLus;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public void setLocalDir(final String localDir) {
		this.localDir = localDir;
	}

	public void setLocator(final String locator) {
		this.locator = locator;
	}

	public void setLus(final boolean isLus) {
		this.isLus = isLus;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setRemoteDir(final String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "InstallationDetails [privateIP=" + privateIp + ", publicIP=" + publicIp
				+ ", locator=" + locator + ", connectToPrivateIP=" + connectedToPrivateIp
				+ ", cloudifyUrl=" + cloudifyUrl
				+ ", bindToPrivateIP=" + bindToPrivateIp
				+ ", username=" + username
				+ ", password=***" + ", keyFile=" + keyFile + ", localDir=" + localDir + ", remoteDir=" + remoteDir
				+ ", isLus=" + isLus + ", zones=" + zones + ", extraRemoteEnvironmentVariables = "
				+ extraRemoteEnvironmentVariables + "]";
	}

	public String getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(final String keyFile) {
		this.keyFile = keyFile;
	}

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(final String privateIp) {
		this.privateIp = privateIp;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(final String publicIp) {
		this.publicIp = publicIp;
	}

	public String getZones() {
		return zones;
	}

	public void setZones(final String zones) {
		this.zones = zones;
	}

	public String[] getManagementOnlyFiles() {
		return this.managementOnlyFiles;
	}

	public void setManagementOnlyFiles(final List<String> managementOnlyFiles) {
		this.managementOnlyFiles = managementOnlyFiles.toArray(new String[managementOnlyFiles.size()]);
	}

	public void setCloudifyUrl(final String cloudifyUrl) {
		this.cloudifyUrl = cloudifyUrl;
	}

	public String getCloudifyUrl() {
		return cloudifyUrl;
	}

	public void setConnectedToPrivateIp(final boolean connectedToPrivateIp) {
		this.connectedToPrivateIp = connectedToPrivateIp;
	}

	public boolean isConnectedToPrivateIp() {
		return connectedToPrivateIp;
	}

	// shallow copy
	@Override
	public InstallationDetails clone() {
		final InstallationDetails result = new InstallationDetails();
		ReflectionUtils.shallowCopyFieldState(this, result);
		return result;
	}

	public void setNoWebServices(final boolean noWebServices) {
		this.noWebServices = noWebServices;
	}

	public boolean isNoWebServices() {
		return noWebServices;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(final String machineId) {
		this.machineId = machineId;
	}

	public File getCloudFile() {
		return cloudFile;
	}

	public void setCloudFile(final File cloudFile) {
		this.cloudFile = cloudFile;
	}

	public String getOverridesUrl() {
		return overridesUrl;
	}

	public void setOverridesUrl(final String overridesUrl) {
		this.overridesUrl = overridesUrl;
	}

	public boolean isBindToPrivateIp() {
		return bindToPrivateIp;
	}

	public void setBindToPrivateIp(final boolean bindToPrivateIp) {
		this.bindToPrivateIp = bindToPrivateIp;
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

	public Map<String, Object> getCustomData() {
		return customData;
	}

	public Map<String, String> getExtraRemoteEnvironmentVariables() {
		return extraRemoteEnvironmentVariables;
	}

	public void setExtraRemoteEnvironmentVariables(final Map<String, String> extraRemoteEnvironmentVariables) {
		this.extraRemoteEnvironmentVariables = extraRemoteEnvironmentVariables;
	}

	public String getRelativeLocalDir() {
		return relativeLocalDir;
	}

	public void setRelativeLocalDir(final String relativeLocalDir) {
		this.relativeLocalDir = relativeLocalDir;
	}

	public GSAReservationId getReservationId() {
		return reservationId;
	}

	public void setReservationId(GSAReservationId reservationId) {
		this.reservationId = reservationId;
	}

}
