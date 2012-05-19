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
package org.cloudifysource.dsl.cloud;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationException;

import com.j_spaces.kernel.Environment;

/***********
 * Cloud doman object. Includes all of the details required for the cloud driver to use a cloud provider.
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
	private Map<String, CloudTemplate> templates = new HashMap<String, CloudTemplate>();
	private Map<String, Object> custom = new HashMap<String, Object>();

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

	public Map<String, CloudTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, CloudTemplate> templates) {
		this.templates = templates;
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
		return "Cloud [name=" + name + ", provider=" + provider + ", user=" + user + ", configuration=" + configuration
				+ ", templates=" + templates + ", custom=" + custom + "]";
	}

	@DSLValidation
	void validateManagementTemplateName()
			throws DSLValidationException {

		CloudConfiguration configuration = getConfiguration();
		Map<String, CloudTemplate> templates = getTemplates();

		String managementTemplateName = configuration.getManagementMachineTemplate();

		if (StringUtils.isBlank(managementTemplateName)) {
			throw new DSLValidationException("managementTemplateName may not be empty");
		}
		
		if (!templates.containsKey(managementTemplateName)) {
			throw new DSLValidationException("The management machine template \"" + managementTemplateName + "\" is "
					+ "not listed in the cloud's templates section");
		}

	}
	
	@DSLValidation
	void validateKeySettings() throws DSLValidationException {
		File keyFile = null;
		String keyFileStr = getUser().getKeyFile();
		if (StringUtils.isNotBlank(keyFileStr)) {
			keyFile = new File(keyFileStr);
			if (!keyFile.isAbsolute()) {
				fixConfigRelativePaths();
				keyFile = new File(getProvider().getLocalDirectory(), keyFileStr);
			}
			if (!keyFile.isFile()) {
				throw new DSLValidationException("The specified key file is missing: \"" 
						+ keyFile.getAbsolutePath() + "\"");
			}
		}	
	}
	
	/**
	 * Sets the localDirectory setting of the given cloud object to an absolute path, based on the home directory.
	 */
	private void fixConfigRelativePaths() {
		String configLocalDir = getProvider().getLocalDirectory();
		if (configLocalDir != null && !new File(configLocalDir).isAbsolute()) {
			String envHomeDir = Environment.getHomeDirectory();
			getProvider().setLocalDirectory(new File(envHomeDir, configLocalDir).getAbsolutePath());
		}
	}

}
