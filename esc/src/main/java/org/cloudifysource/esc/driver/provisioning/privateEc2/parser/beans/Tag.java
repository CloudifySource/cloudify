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
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents EC2Tag node of Amazon CloudFormation.<br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-tags.html"
 * >http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-tags.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class Tag {

	@JsonProperty("Key")
	private String key;

	@JsonProperty("Value")
	private String value;

	public Tag() {
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Convert this object to the Amazon Tag.
	 * 
	 * @return The Amazon tag object.
	 */
	public com.amazonaws.services.ec2.model.Tag convertToEC2Model() {
		return new com.amazonaws.services.ec2.model.Tag(key, value);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
