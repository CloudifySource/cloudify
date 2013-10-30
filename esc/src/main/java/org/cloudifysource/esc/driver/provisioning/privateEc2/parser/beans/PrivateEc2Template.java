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
package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers.ResourcesDeserializer;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * This class represents the root node of the Amazon CloudFormation Template.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class PrivateEc2Template {

	@JsonProperty("Resources")
	@JsonDeserialize(using = ResourcesDeserializer.class)
	private List<AWSResource> resources;

	public void setResources(final List<AWSResource> resources) {
		this.resources = resources;
	}

	public List<AWSResource> getResources() {
		return resources;
	}

	public AWSEC2Instance getEC2Instance() {
		return this.getResourceType(AWSEC2Instance.class, null);
	}

	/**
	 * Returns a volume bean from the given volume name.<br />
	 * 
	 * @param volumeName
	 *            The volume name of the <code>AWS::EC2::Volume</code> node to return.
	 * @return Returns a volume bean from the given volume name.<br />
	 *         If no volume name is given, it will return the first volume found in the list of resources.
	 */
	public AWSEC2Volume getEC2Volume(final String volumeName) {
		return this.getResourceType(AWSEC2Volume.class, volumeName);
	}

	@SuppressWarnings("unchecked")
	private <T> T getResourceType(final Class<T> clazz, final String volumeName) {
		for (AWSResource resource : this.resources) {
			if (clazz.isInstance(resource)) {
				if (volumeName != null) {
					if (volumeName.equals(resource.getResourceName())) {
						return (T) resource;
					}
				} else {
					return (T) resource;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
