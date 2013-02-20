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
package org.cloudifysource.shell.proxy;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;

import com.sun.deploy.net.proxy.DeployProxySelector;
import com.sun.deploy.net.proxy.DynamicProxyManager;
import com.sun.deploy.services.PlatformType;
import com.sun.deploy.services.ServiceManager;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        This extension of the {@link DeployProxySelector} sets this class as the proxy selector. Failed
 *        connections are suppressed, to avoid GUI error messages to pop-up.
 * 
 */
public class SystemDefaultProxySelector extends DeployProxySelector {

	/**
	 * Sets up {@link SystemDefaultProxySelector} as the system-wide proxy selector.
	 */
	public static void setup() {

		// Use windows (other platforms might work, though this hasen't been tested)
		ServiceManager.setService(PlatformType.STANDALONE_TIGER_WIN32);

		// Go fetch to system proxy settings
		DynamicProxyManager.reset();

		// When connection requests are made, use me as your proxy selector
		ProxySelector.setDefault(new SystemDefaultProxySelector());

	}

	// Without this override, failure to connect will cause a gui error window to pop
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
		// Do nothing
	}

}