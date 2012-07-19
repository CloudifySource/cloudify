/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.jmx;

/**
 * Simple object for JMX Attribute.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class JmxAttribute implements Comparable<JmxAttribute> {

	private String objectName;
	private String attributeName;
	private String displayName;
	private Object value;

	public JmxAttribute(final String objectName, final String attributeName, final String displayName) {
		super();
		this.objectName = objectName;
		this.attributeName = attributeName;
		this.displayName = displayName;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(final String objectName) {
		this.objectName = objectName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(final String attributeName) {
		this.attributeName = attributeName;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return "JmxAttribute [objectName=" + objectName + ", attributeName=" + attributeName + ", displayName="
				+ displayName + "]";
	}

	@Override
	public int compareTo(final JmxAttribute o) {
		final int beanComparison = this.getObjectName().compareTo(o.getObjectName());
		if (beanComparison != 0) {
			return beanComparison;
		}
		return this.getAttributeName().compareTo(o.getAttributeName());
	}

}
