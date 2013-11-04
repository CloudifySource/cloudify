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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author victor
 * @since 2.7.0
 */
public class TokenServiceCatalogEndpoint {
	private String adminURL;
	private String region;
	private String internalURL;
	private String id;
	private String publicURL;

	public String getAdminURL() {
		return adminURL;
	}

	public void setAdminURL(final String adminURL) {
		this.adminURL = adminURL;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(final String region) {
		this.region = region;
	}

	public String getInternalURL() {
		return internalURL;
	}

	public void setInternalURL(final String internalURL) {
		this.internalURL = internalURL;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getPublicURL() {
		return publicURL;
	}

	public void setPublicURL(final String publicURL) {
		this.publicURL = publicURL;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
