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
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a bean representing the properties of AWS::EC2::Volume of Amazon CloudFormation.<br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-ebs-volume.html">
 * http://docs .aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-ebs-volume.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class VolumeProperties {

	@JsonProperty("AvailabilityZone")
	private ValueType availabilityZone;

	@JsonProperty("Iops")
	private Integer iops;

	@JsonProperty("Size")
	private Integer size;

	@JsonProperty("SnapshotId")
	private ValueType snapshotId;

	@JsonProperty("Tags")
	private List<Tag> tags;

	@JsonProperty("VolumeType")
	private ValueType volumeType;

	public Integer getSize() {
		return size;
	}

	public ValueType getAvailabilityZone() {
		return availabilityZone;
	}

	public void setSize(final Integer size) {
		this.size = size;
	}

	public Integer getIops() {
		return iops;
	}

	public ValueType getSnapshotId() {
		return snapshotId;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public ValueType getVolumeType() {
		return volumeType;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
