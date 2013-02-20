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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Generic fetcher for external-process JMX data.
 *
 * @author giladh
 * @since 8.0.1
 *
 */
public class JmxGenericClient {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(JmxGenericClient.class.getName());

	private static final int DEFAULT_JMX_PORT = 8080;

	private static final String JMX_URL_FORMAT = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";

	private int port = DEFAULT_JMX_PORT;

	private String host = "127.0.0.1";

	private ArrayList<JmxBeanAttributes> targetList = new ArrayList<JmxBeanAttributes>();

	private String username;
	private String password;

	private int numOfTargets;

	public void setHost(final String host) {
		this.host = host.trim();
	}

	public void setPort(final int port) {
		this.port = port;

	}

	/********
	 *
	 * @author barakme
	 *
	 */
	private static class JmxBeanAttributes {

		private String objectName = "";
		private final List<JmxAttribute> attributes = new LinkedList<JmxAttribute>();
		private final Map<String, JmxAttribute> attributesByName = new HashMap<String, JmxAttribute>();

		public JmxBeanAttributes(final String objectName) {
			this.objectName = objectName;
		}

		public String getObjectName() {
			return objectName;
		}

		public void add(final JmxAttribute att) {
			this.attributes.add(att);
			this.attributesByName.put(att.getAttributeName(), att);

		}

		public String[] getAttributeNames() {
			final String[] arr = new String[this.attributes.size()];
			int i = 0;
			for (final JmxAttribute att : this.attributes) {
				arr[i] = att.getAttributeName();
				++i;
			}
			return arr;

		}

		public JmxAttribute setValueAndReturnAttribute(final String name, final Object value) {
			final JmxAttribute att = this.attributesByName.get(name);
			if (att == null) {
				throw new java.lang.IllegalStateException(
						"Attempted to set value of JMX attribute that does not exist. Attribute name: " + name
								+ ", bean name: " + this.objectName);
			}
			att.setValue(value);

			return att;
		}

		@Override
		public String toString() {
			return "JmxBeanAttributes [beanName=" + objectName + ", attributes=" + attributes + "]";
		}

	}

	public void setTargets(final List<JmxAttribute> list) {
		// Sort the input list by bean name and attribute Name
		Collections.sort(list);

		this.targetList = new ArrayList<JmxGenericClient.JmxBeanAttributes>();

		JmxBeanAttributes current = null;
		for (final JmxAttribute jmxAttribute : list) {
			if (current == null ||
					!jmxAttribute.getObjectName().equals(current.getObjectName())) {
				current = new JmxBeanAttributes(jmxAttribute.getObjectName());
				this.targetList.add(current);
			}

			current.add(jmxAttribute);
		}

		this.numOfTargets = list.size();
	}

	public ArrayList<JmxAttribute> getData() {

		final JMXServiceURL jmxUrl = createJMXServiceURL();

		JMXConnector jmxc = null;

		final Map<String, Object> env = createEnvironment();

		final ArrayList<JmxAttribute> resultList = new ArrayList<JmxAttribute>(this.numOfTargets);

		try {
			jmxc = JMXConnectorFactory.connect(jmxUrl, env);
			final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

			for (final JmxBeanAttributes t : targetList) {

				handleJMXBean(resultList, mbsc, t);

			}

			return resultList;
		} catch (final Exception e) {
			final String msg = "Failed to fetch JMX values for " + host + ":" + port + ". Error: " + e;
			logger.severe(msg);
		} finally {
			if (jmxc != null) {
				try {
					jmxc.close();
				} catch (final IOException e) {
				}
			}
		}
		return null;
	}

	protected void handleJMXBean(final ArrayList<JmxAttribute> resultList, final MBeanServerConnection mbsc,
			final JmxBeanAttributes t)
			throws MalformedObjectNameException {

		final ObjectName beanName = new ObjectName(t.getObjectName());
		final String[] attributeNames = t.getAttributeNames();

		try {
			// This is the remote call!
			// Object val = mbsc.getAttribute(beanName, attributeNames[0]);
			final AttributeList vals = mbsc.getAttributes(beanName, attributeNames);
            for (Object val : vals) {
                final Attribute att = (Attribute) val;
                if (att.getValue() instanceof Exception) {
                    logger.log(Level.WARNING, "Failed to read JMX attribute: " + att.getName() + " in bean: "
                            + t.getObjectName(), att.getValue());
                } else {
                    final JmxAttribute result = t.setValueAndReturnAttribute(att.getName(), att.getValue());
                    resultList.add(result);
                }
            }

		} catch (final Exception e) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.warning("Failed to read Attributes for JMX Bean: " + t + ": " + e.getMessage());
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Failed to read Attributes for JMX Bean: " + t, e);
			}
		}
	}

    /**
     * Create a jmx client
     * @return    a jmx client
     */
	private JMXServiceURL createJMXServiceURL() {
		try {
			return new JMXServiceURL(String.format(JMX_URL_FORMAT, host, port));
		} catch (final MalformedURLException e) {
			// none recoverable
			final String msg = "Failed to create JMXServiceURL for " + host + "," + port + ". Error: " + e;
			logger.severe(msg);
			throw new IllegalArgumentException("Failed to initialize JMX Service URL: "
					+ String.format(JMX_URL_FORMAT, host, port), e);
		}
	}

	private Map<String, Object> createEnvironment() {
		final Map<String, Object> env = new HashMap<String, Object>();

		if (this.hasCredentials()) {
			final String[] credentials = new String[] { username, password };
			env.put("jmx.remote.credentials", credentials);

		}
		return env;
	}

	private boolean hasCredentials() {
		return this.username != null || this.password != null;

	}

	public Map<String, Object> getAttributes() {

		final ArrayList<JmxAttribute> resArr = getData();

		final Map<String, Object> results = new HashMap<String, Object>();
		if (resArr == null) {
			return results;
		}
		String name;
		for (final JmxAttribute t : resArr) {
			name = t.getDisplayName();
			if (name == null || name.isEmpty()) {
				name = t.getAttributeName();
			}
			results.put(name, t.getValue());
		}

		return results;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}
}
