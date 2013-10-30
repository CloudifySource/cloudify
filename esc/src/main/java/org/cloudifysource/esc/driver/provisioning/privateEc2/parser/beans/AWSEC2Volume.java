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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a bean representing a EC2 Volume resource node of the Amazon CloudFormation template.<br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-ebs-volume.html">
 * http://docs .aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-ebs-volume.html</a>
 * 
 * @author victor
 * @since 2.7.0
 */
public class AWSEC2Volume extends AWSResource {

	@JsonProperty("Properties")
	private VolumeProperties properties;

	public VolumeProperties getProperties() {
		return properties;
	}

	public void setProperties(final VolumeProperties properties) {
		this.properties = properties;
	}
}
