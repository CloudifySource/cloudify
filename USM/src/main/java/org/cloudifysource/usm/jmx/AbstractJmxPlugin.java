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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.cloudifysource.dsl.Plugin;
import org.cloudifysource.dsl.context.ServiceContext;

/***************
 * A base class for plugins that read JMX data.
 * 
 * @author barakme
 * 
 */

public abstract class AbstractJmxPlugin implements Plugin {

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(AbstractJmxPlugin.class.getName());

	// protected List<JmxTarget> targets = new LinkedList<JmxTarget>();
	protected List<JmxAttribute> targets = new LinkedList<JmxAttribute>();

	protected String host = "127.0.0.1";
	protected int port;
	protected JmxGenericClient client;

	protected String username;
	protected String password;

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	@Override
	public void setConfig(final Map<String, Object> config) {

		final Set<Entry<String, Object>> entries = config.entrySet();
		for (final Entry<String, Object> entry : entries) {
			try {

				if (entry.getKey().equalsIgnoreCase("port")) {
					this.setPort(Integer.parseInt(entry.getValue().toString()));
				} else if (entry.getKey().equalsIgnoreCase("host")) {
					this.setHost((String) entry.getValue());
				} else if (entry.getKey().equalsIgnoreCase("username")) {
					this.username = (String) entry.getValue();
				} else if (entry.getKey().equalsIgnoreCase("password")) {
					this.password = (String) entry.getValue();
				} else {

					final List<?> list = (List<?>) entry.getValue();
					final JmxAttribute att =
							new JmxAttribute(list.get(0).toString(), list.get(1).toString(), entry.getKey());
					this.targets.add(att);
					// final JmxTarget target = JmxTargetParser.parse((String) entry.getValue());
					// target.setDispName(entry.getKey());
					// this.targets.add(target);

				}

			} catch (final Exception e) {
				logger.log(Level.SEVERE,
						"Failed to process Jmx Configuration entry: " + entry.getKey() + "=" + entry.getValue(), e);

			}
		}

	}

	public List<JmxAttribute> getTargets() {
		return targets;
	}

	public void setTargets(final List<JmxAttribute> targets) {
		this.targets = targets;
	}

	/**********
	 * Collects and returns the JMX data.
	 * @return the JMX data.
	 */
	protected Map<String, Object> getJmxAttributes() {
		if (this.client == null) {
			client = new JmxGenericClient();
			client.setHost(this.host);
			client.setPort(this.port);
			client.setTargets(this.targets);
		}

		return client.getAttributes();
	}

	@Override
	public void setServiceContext(final ServiceContext context) {
		// ignore
	}

}
