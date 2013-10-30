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
package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A bean to handle the parsing of the <code>Fn::Join</code> function of Amazon CloudFormation.<br />
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-join.html">
 * http:// docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-join.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class JoinFunction implements ValueType {

	private final String separator;

	private final List<ValueType> strings;

	public JoinFunction(final String separator, final List<ValueType> strings) {
		this.separator = separator;
		this.strings = strings;
	}

	@Override
	public String getValue() {
		StringBuilder sb = new StringBuilder();
		for (ValueType s : this.strings) {
			sb.append(s.getValue()).append(this.separator);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
