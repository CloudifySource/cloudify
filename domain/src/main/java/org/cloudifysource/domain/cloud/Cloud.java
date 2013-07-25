/*******************************************************************************
' * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.domain.cloud;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.cloudifysource.domain.cloud.compute.CloudCompute;
import org.cloudifysource.domain.cloud.storage.CloudStorage;
import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/***********
 * Cloud domain object. Includes all of the details required for the cloud driver to use a cloud provider.
 *
 * @author barakme
 *
 */
@CloudifyDSLEntity(name = "cloud", clazz = Cloud.class, allowInternalNode = false, allowRootNode = true)
public class Cloud {

	private String name;
	private CloudProvider provider;
	private CloudUser user = new CloudUser();
	private CloudConfiguration configuration = new CloudConfiguration();
	private Map<String, Object> custom = new HashMap<String, Object>();
	private CloudCompute cloudCompute = new CloudCompute();
	private CloudStorage cloudStorage = new CloudStorage();

	public CloudStorage getCloudStorage() {
		return cloudStorage;
	}

	public void setCloudStorage(final CloudStorage cloudStorage) {
		this.cloudStorage = cloudStorage;
	}

	public CloudCompute getCloudCompute() {
		return cloudCompute;
	}

	public void setCloudCompute(final CloudCompute cloudCompute) {
		this.cloudCompute = cloudCompute;
	}

	// CIFS drive regex (for example: /C$ or /d$)
	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "^/[a-zA-Z][$]/.*";

	public Map<String, Object> getCustom() {
		return custom;
	}

	public void setCustom(final Map<String, Object> custom) {
		this.custom = custom;
	}

	public CloudProvider getProvider() {
		return provider;
	}

	public void setProvider(final CloudProvider provider) {
		this.provider = provider;
	}

	public CloudUser getUser() {
		return user;
	}

	public void setUser(final CloudUser user) {
		this.user = user;
	}

	public CloudConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final CloudConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Cloud [name=" + name + ", provider=" + provider + ", user=" + user + ", configuration="
				+ configuration + ", cloudCompute=" + cloudCompute + ", custom=" + custom + "]";
	}



	// moved this into template object
	/**
	 * This validation method runs both locally and on the remote server.
	 *
	 * @throws DSLValidationException
	 */
	// @DSLValidation
	// void validateKeySettings(final DSLValidationContext validationContext)
	// throws DSLValidationException {
	// File keyFile = null;
	//
	// for (CloudTemplate template : this.templates.values()) {
	//
	// String keyFileStr = template.getKeyFile();
	// if (StringUtils.isNotBlank(keyFileStr)) {
	// keyFile = new File(keyFileStr);
	//
	// if (!keyFile.isAbsolute()) {
	//
	// //final File localDir = new File(validationContext.getFilePath()).getParentFile();
	// // expected to be absolute path at this point
	// final File uploadDir = new File(template.getLocalDirectory());
	// final File absoluteKeyFile = new File(uploadDir, keyFileStr);
	// if (!absoluteKeyFile.exists()) {
	// throw new DSLValidationException("The specified key file was not found: " + absoluteKeyFile);
	// }
	//
	// // String configLocalDir = template.getLocalDirectory();
	// // if (configLocalDir != null && !new File(configLocalDir).isAbsolute()) {
	// // boolean keyFileFoundOnLocalMachinePath = isKeyFileFoundOnLocalMachinePath(template);
	// // boolean keyFileFoundOnRemoteMachinePath = isKeyFileFoundOnRemoteMachinePath(template);
	// // if (!keyFileFoundOnRemoteMachinePath && !keyFileFoundOnLocalMachinePath) {
	// // throw new DSLValidationException(
	// // "The specified key file is not found on these locations: \""
	// // + (new File(getLocalDirPath(template), keyFileStr)).getAbsolutePath()
	// // + "\", \""
	// // + (new File(getRemoteDirPath(template), keyFileStr)).getAbsolutePath()
	// // + "\"");
	// // }
	// // }
	// } else {
	// if (!keyFile.isFile()) {
	// throw new DSLValidationException("The specified key file is missing: \""
	// + keyFile.getAbsolutePath() + "\"");
	// }
	// }
	// }
	// }
	// }

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute CIFS path, returns /PATH. If the string does not
	 * match, returns the original unmodified string.
	 *
	 * @param path the input path.
	 * @return the input path, adjusted to remove the CIFS drive letter, if it exists, or the original path if the drive
	 *         letter is not present.
	 */
	public static String normalizeCifsPath(final String path) {
		final String expression = CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX;
		final Pattern pattern = Pattern.compile(expression);

		if (pattern.matcher(path).matches()) {
			final char drive = path.charAt(1);
			return drive + ":\\" + path.substring("/c$/".length()).replace('/', '\\');
		}
		return path;
	}

	// private boolean isKeyFileFoundOnRemoteMachinePath(final CloudTemplate template) {
	// String remoteEnvDirectoryPath = getRemoteDirPath(template);
	// File remoteKeyFile = new File(remoteEnvDirectoryPath, template.getKeyFile());
	// logger.log(Level.FINE, "Looking for key file on remote machine: " + remoteKeyFile.getAbsolutePath());
	// return remoteKeyFile.isFile();
	// }
	//
	// private boolean isKeyFileFoundOnLocalMachinePath(final CloudTemplate template) {
	// String localAbsolutePath = getLocalDirPath(template);
	// File localKeyFile = new File(localAbsolutePath, template.getKeyFile());
	// logger.log(Level.FINE, "Looking for key file on local machine: " + localKeyFile.getAbsolutePath());
	// return localKeyFile.isFile();
	// }

	// private String getLocalDirPath(final CloudTemplate template) {
	// String configLocalDir = template.getLocalDirectory();
	// // getting the local config directory
	// String envHomeDir = Environment.getHomeDirectory();
	// return new File(envHomeDir, configLocalDir).getAbsolutePath();
	// }
	//
	// private String getRemoteDirPath(final CloudTemplate template) {
	// // String managementMachineTemplateName = getConfiguration().getManagementMachineTemplate();
	// String remoteEnvDirectoryPath = template.getRemoteDirectory();
	// // fix the remote path if formatted for vfs2, so it would be parsed correctly.
	// return normalizeCifsPath(remoteEnvDirectoryPath);
	// }
}