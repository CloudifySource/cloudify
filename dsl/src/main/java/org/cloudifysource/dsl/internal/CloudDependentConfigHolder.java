/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

/**
 * Complementary config holder.
 * @author adaml
 *
 */
public class CloudDependentConfigHolder {

	private Integer defaultLusPort;
	private String downloadUrl;
	
	public String getDownloadUrl() {
		return downloadUrl;
	}
	
	public void setDownloadUrl(final String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public Integer getDefaultLusPort() {
		return defaultLusPort;
	}

	public void setDefaultLusPort(final Integer defaultLusPort) {
		this.defaultLusPort = defaultLusPort;
	}

}
