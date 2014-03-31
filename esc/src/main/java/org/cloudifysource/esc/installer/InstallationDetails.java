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
package org.cloudifysource.esc.installer;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudifysource.domain.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.RemoteExecutionModes;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.dsl.internal.CloudifyConstants;
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
	
	// allow agent/agent-machine restart 
	private String autoRestartAgent;

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

	// security profile
	private String securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;

	// keystore password
	private String keystorePassword;

	// An instance of the Gigaspaces Admin API. If passed,
	// will be used to check when an agent joins the cluster.
	private Admin admin;

	// true if this machine should act as a Cloudify Manager
	private boolean isManagement;

	// (only relevant in case isLus == true) if true no web-services will be
	// deployed on the target machine
	private boolean noWebServices;

	// (only relevant in case isLus == true) if true no cloudify management space will be
	// deployed on the target machine
	private boolean noManagementSpace;
	
	// (only relevant in case isLus == true) if true no container for the cloudify management space will be
	// deployed on the target machine
	private boolean noManagementSpaceContainer;
	
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

	// wherther we are in the same network as the machine we are about to
	// install
	private boolean connectedToPrivateIp;

	// whether the NIC_ADDR of the machine should be the private or public IP
	private boolean bindToPrivateIp = true;

	// a cloud specific identifier for a host
	private String machineId;

	private File cloudFile;

	private FileTransferModes fileTransferMode = FileTransferModes.SFTP;
	private RemoteExecutionModes remoteExecutionMode = RemoteExecutionModes.SSH;
	private ScriptLanguages scriptLanguage = ScriptLanguages.LINUX_SHELL;

	private final Map<String, Object> customData = new HashMap<String, Object>();

	private Map<String, String> extraRemoteEnvironmentVariables = new LinkedHashMap<String, String>();

	// defines the com.gs.agent.reservationid system property for the GSA
	// see InternalGridServiceAgent#getReservationId()
	private GSAReservationId reservationId;

	private String templateName;

	private String authGroups;

	private CloudTemplateInstallerConfiguration installerConfiguration = null;
	// Relevant only for management machines
	/* *********************************************** */

	// The management components system properties
	// as java command line arguments
	private String esmCommandlineArgs;
	private String lusCommandlineArgs;
	private String gsmCommandlineArgs;
	private String gsaCommandlineArgs;
	private String gscLrmiPortRange;

	// management web service properties
	private String restMaxMemory;
	private String webuiMaxMemory;
	private Integer restPort;
	private Integer webuiPort;
	private Integer attributesStoreDiscoveryTimeout;

	// persistent management
	private boolean persistent = false;
	private String persistentStoragePath = null;

	private boolean deleteRemoteDirectoryContents = false;

	private String locationId;

	// indicates that this installation is a re-bootstrapping
	private boolean rebootstrapping = false;

	private String openFilesLimit;

	/*********
	 * Default constructor.
	 */
	public InstallationDetails() {

	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(final String locationId) {
		this.locationId = locationId;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

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

	public boolean isManagement() {
		return isManagement;
	}

	/*****
	 * An instance of the Admin API, used only by instances of the cloud driver running in the cloudify manager. For
	 * cloud driver instances running in the Cloudify CLI (for bootstrapping/teardown) this value is null.
	 * 
	 * @param admin
	 *            the admin instance.
	 */
	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public void setLocalDir(final String localDir) {
		this.localDir = localDir;
	}

	public void setLocator(final String locator) {
		this.locator = locator;
	}

	public void setManagement(final boolean isManagement) {
		this.isManagement = isManagement;
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
		return "InstallationDetails [privateIP=" + privateIp + ", publicIP=" + publicIp + ", locator=" + locator
				+ ", connectToPrivateIP=" + connectedToPrivateIp + ", cloudifyUrl=" + cloudifyUrl
				+ ", bindToPrivateIP=" + bindToPrivateIp + ", username=" + username + ", password=***" + ", keyFile="
				+ keyFile + ", localDir=" + localDir + ", remoteDir=" + remoteDir + ", isLus=" + isManagement
				+ ", zones="
				+ zones + ", extraRemoteEnvironmentVariables = " + extraRemoteEnvironmentVariables;
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

	/********
	 * Set the list of files that should only be copied to management machines, not agent ones. '\' characters are
	 * replaced with '/' to make string comparisons easier.
	 * 
	 * @param managementOnlyFiles
	 *            the list of files.
	 */
	public void setManagementOnlyFiles(final List<String> managementOnlyFiles) {

		// copy list into array - make sure to use '/' as separator char for string comparisons later on.
		this.managementOnlyFiles = new String[managementOnlyFiles.size()];
		final int i = 0;
		for (final String string : managementOnlyFiles) {
			this.managementOnlyFiles[i] = string.replace("\\", "/");
		}

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

	/*****
	 * Indicates if the cloudify processes running on the new machine should bind to the private ip or to the public
	 * one. Default to true (bind to private IP).
	 * 
	 * @param bindToPrivateIp
	 *            .
	 */
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

	public void setReservationId(final GSAReservationId reservationId) {
		this.reservationId = reservationId;
	}

	public String getSecurityProfile() {
		return securityProfile;
	}

	public void setSecurityProfile(final String securityProfile) {
		this.securityProfile = securityProfile;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(final String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public String getAuthGroups() {
		return this.authGroups;
	}

	public String getEsmCommandlineArgs() {
		return esmCommandlineArgs;
	}

	public void setEsmCommandlineArgs(final String esmCommandlineArgs) {
		this.esmCommandlineArgs = esmCommandlineArgs;
	}

	public String getLusCommandlineArgs() {
		return lusCommandlineArgs;
	}

	public void setLusCommandlineArgs(final String lusCommandlineArgs) {
		this.lusCommandlineArgs = lusCommandlineArgs;
	}

	public String getGsmCommandlineArgs() {
		return gsmCommandlineArgs;
	}

	public void setGsmCommandlineArgs(final String gsmCommandlineArgs) {
		this.gsmCommandlineArgs = gsmCommandlineArgs;
	}

	public String getGsaCommandlineArgs() {
		return gsaCommandlineArgs;
	}

	public void setGsaCommandlineArgs(final String gsaCommandlineArgs) {
		this.gsaCommandlineArgs = gsaCommandlineArgs;
	}

	public String getGscLrmiPortRange() {
		return gscLrmiPortRange;
	}

	public void setGscLrmiPortRange(final String gscLrmiPortRange) {
		this.gscLrmiPortRange = gscLrmiPortRange;
	}

	public Integer getRestPort() {
		return restPort;
	}

	public void setRestPort(final Integer restPort) {
		this.restPort = restPort;
	}

	public Integer getWebuiPort() {
		return webuiPort;
	}

	public void setWebuiPort(final Integer webuiPort) {
		this.webuiPort = webuiPort;
	}

	public String getRestMaxMemory() {
		return restMaxMemory;
	}

	public void setRestMaxMemory(final String restMaxMemory) {
		this.restMaxMemory = restMaxMemory;
	}

	public String getWebuiMaxMemory() {
		return webuiMaxMemory;
	}

	public void setWebuiMaxMemory(final String webuiMaxMemory) {
		this.webuiMaxMemory = webuiMaxMemory;
	}
	
	public Integer getAttributesStoreDiscoveryTimeout() {
		return attributesStoreDiscoveryTimeout;
	}
	
	public void setAttributesStoreDiscoveryTimeout(final Integer attributesStoreDiscoveryTimeoutInSeconds) {
		this.attributesStoreDiscoveryTimeout = attributesStoreDiscoveryTimeoutInSeconds;
	}

	public ScriptLanguages getScriptLanguage() {
		return scriptLanguage;
	}

	public void setScriptLanguage(final ScriptLanguages scriptLanguagee) {
		this.scriptLanguage = scriptLanguagee;
	}

	public boolean isDeleteRemoteDirectoryContents() {
		return deleteRemoteDirectoryContents;
	}

	public void setDeleteRemoteDirectoryContents(final boolean deleteRemoteDirectoryContents) {
		this.deleteRemoteDirectoryContents = deleteRemoteDirectoryContents;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(final boolean persistent) {
		this.persistent = persistent;
	}

	public String getPersistentStoragePath() {
		return persistentStoragePath;
	}

	public void setPersistentStoragePath(final String persistentStoragePath) {
		this.persistentStoragePath = persistentStoragePath;
	}

	public CloudTemplateInstallerConfiguration getInstallerConfiguration() {
		return installerConfiguration;
	}

	public void setInstallerConfiguration(final CloudTemplateInstallerConfiguration installerConfiguration) {
		this.installerConfiguration = installerConfiguration;
	}

	public boolean isRebootstrapping() {
		return rebootstrapping;
	}

	public void setRebootstrapping(final boolean rebootstrapping) {
		this.rebootstrapping = rebootstrapping;
	}

	public String getOpenFilesLimit() {
		return openFilesLimit;
	}

	public void setOpenFilesLimit(final String openFilesLimit) {
		this.openFilesLimit = openFilesLimit;
	}

	public String getAutoRestartAgent() {
		return this.autoRestartAgent;
	}

	public void setAutoRestartAgent(final String autoRestartAgent) {
		this.autoRestartAgent = autoRestartAgent;
	}

	public boolean isNoManagementSpace() {
		return noManagementSpace;
	}
	
	public void setNoManagementSpace(boolean noCloudifyManagementSpace) {
		this.noManagementSpace = noCloudifyManagementSpace;
	}

	public void setNoManagementSpaceContainer(boolean noCloudifyManagementSpaceContainer) {
		this.noManagementSpaceContainer = noCloudifyManagementSpaceContainer;
	}

	public boolean isNoManagementSpaceContainer() {
		return noManagementSpaceContainer;
	}
}
