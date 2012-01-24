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
