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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.RefValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents an EC2 MountPoint of Amazon CloudFormation.<br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-mount-point.html">http://
 * docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-mount-point.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class VolumeMapping {

	@JsonProperty("VolumeId")
	private RefValue volumeId;

	@JsonProperty("Device")
	private ValueType device;

	public ValueType getVolumeId() {
		return volumeId;
	}

	public ValueType getDevice() {
		return device;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
