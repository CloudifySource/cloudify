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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a bean representing the properties of AWS::EC2::Instance in Amazon CloudFormation template.<br />
 * Be aware that all properties are not supported. <br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-instance.html"
 * >http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-instance.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class InstanceProperties {

	@JsonProperty("AvailabilityZone")
	private ValueType availabilityZone;

	@JsonProperty("ImageId")
	private ValueType imageId;

	@JsonProperty("InstanceType")
	private ValueType instanceType;

	@JsonProperty("KeyName")
	private ValueType keyName;

	@JsonProperty("PrivateIpAddress")
	private ValueType privateIpAddress;

	@JsonProperty("SecurityGroupsIds")
	private List<ValueType> securityGroupIds;

	@JsonProperty("SecurityGroups")
	private List<ValueType> securityGroups;

	@JsonProperty("Tags")
	private List<Tag> tags;

	@JsonProperty("UserData")
	private ValueType userData;

	@JsonProperty("Volumes")
	private List<VolumeMapping> volumes;

	public ValueType getImageId() {
		return imageId;
	}

	public ValueType getInstanceType() {
		return instanceType;
	}

	public ValueType getAvailabilityZone() {
		return availabilityZone;
	}

	public List<ValueType> getSecurityGroupIds() {
		return securityGroupIds;
	}

	/**
	 * Get a list of security groups id as string.
	 * 
	 * @return a list of security groups id as string.
	 */
	public List<String> getSecurityGroupIdsAsString() {
		if (securityGroupIds == null) {
			return null;
		} else {
			ArrayList<String> arrayList = new ArrayList<String>(this.securityGroupIds.size());
			for (ValueType value : securityGroupIds) {
				arrayList.add(value.getValue());
			}
			return arrayList;
		}
	}

	public List<ValueType> getSecurityGroups() {
		return securityGroups;
	}

	/**
	 * Get a list of security groups as string.
	 * 
	 * @return a list of security groups as string.
	 */
	public List<String> getSecurityGroupsAsString() {
		if (securityGroups == null) {
			return null;
		} else {
			ArrayList<String> arrayList = new ArrayList<String>(this.securityGroups.size());
			for (ValueType value : securityGroups) {
				arrayList.add(value.getValue());
			}
			return arrayList;
		}
	}

	public ValueType getKeyName() {
		return keyName;
	}

	public List<VolumeMapping> getVolumes() {
		return volumes;
	}

	public ValueType getUserData() {
		return userData;
	}

	public ValueType getPrivateIpAddress() {
		return privateIpAddress;
	}

	public List<Tag> getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
