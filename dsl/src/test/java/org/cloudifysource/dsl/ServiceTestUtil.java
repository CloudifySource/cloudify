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
package org.cloudifysource.dsl;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Assert;

public class ServiceTestUtil {

	// private constructor to prevent instantiation
	private ServiceTestUtil() {
	}

	static public void validateIcon(Service service, String serviceFilePath) throws Exception {

		String icon = service.getIcon();
		if (icon.startsWith("http")) {
			HttpURLConnection connection = (HttpURLConnection) new URL(icon).openConnection();
			connection.setRequestMethod("HEAD");
			Assert.assertEquals("The icon URL cannot establish a connection", HttpURLConnection.HTTP_OK,
					connection.getResponseCode());
			connection.disconnect();
		}else{
			File iconFile = new File(serviceFilePath, service.getIcon());
			Assert.assertTrue("Icon file not found in location: " + iconFile.getAbsolutePath(), iconFile.exists());
		}
	}

	static public void validateName(Service service, String serviceName) throws Exception {
		Assert.assertNotNull(service);
		Assert.assertTrue("Service name isn't correct", service.getName().compareTo(serviceName) == 0);
	}
}
