/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.azure;

public class AzureCredentials {

	public String getBlobStorageAccountName() {
		return getMandatorySystemProperty("azure.blob.accountname");
	}

	public String getBlobStorageAccountKey() {
		return getMandatorySystemProperty("azure.blob.accountkey");
	}

	public String getHostedServicesCertificateThumbrint() {
		return getMandatorySystemProperty("azure.services.certificate");
	}

	public String getHostedServicesSubscriptionId() {
		return getMandatorySystemProperty("azure.services.subscription");
	}

	private String getMandatorySystemProperty(String systemPropertyName) {
		String value = System.getProperty(systemPropertyName);
		if (value == null) {
			throw new IllegalStateException("The system property " + systemPropertyName + " is undefined.");
		}
		return value;
	}
}
