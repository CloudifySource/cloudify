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

/**
 * A bean to handle the parsing of the <code>Ref</code> function of Amazon CloudFormation.<br />
 * 
 * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-ref.html">http://
 * docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-ref.html</a>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class RefValue implements ValueType {

	private String resourceName;

	public RefValue(final String resourceName) {
		this.resourceName = resourceName;
	}

	@Override
	public String getValue() {
		return this.resourceName;
	}

	@Override
	public String toString() {
		return "Ref=" + resourceName;
	}

}
