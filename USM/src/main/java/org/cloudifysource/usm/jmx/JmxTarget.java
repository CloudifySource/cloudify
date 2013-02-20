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
 * A user-defined JMX target make of type, attribute and display name.
 * 
 * @author giladh
 * @since 8.0.1
 * 
 */
public class JmxTarget {

	private String domain;
	private String type;
	private String attr;
	private String dispName;

	private Object value; // place-holder for the attribute's value

	public JmxTarget(final String domain, final String type, final String attr) {
		this(domain, type, attr, attr/* =default display name */);
	}

	public JmxTarget() {

	}

	public JmxTarget(final String domain, final String type, final String attr, final String dispName) {
		this.domain = domain;
		this.type = type;
		this.attr = attr;
		this.dispName = dispName;
		this.value = null;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(final String domain) {
		this.domain = domain;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getAttr() {
		return attr;
	}

	public void setAttr(final String attr) {
		this.attr = attr;
	}

	public String getDispName() {
		return dispName;
	}

	public void setDispName(final String dispName) {
		this.dispName = dispName;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if (dispName == null) {
			return domain + ":" + type + ":" + attr;
		}
		return domain + ":" + type + ":" + attr + ":" + dispName;
	}
}
