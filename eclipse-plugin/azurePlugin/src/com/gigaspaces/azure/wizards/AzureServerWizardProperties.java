package com.gigaspaces.azure.wizards;

/**
 * Statically holds windows azure server creation wizard properties.
 * The properties are updated after the user presses the wizard's "finish" button.
 * 
 * @author idan
 *
 */
public class AzureServerWizardProperties {

	private static String jreLocation;
	private static String webServerLocation;
	private static String webServerPort;

	protected AzureServerWizardProperties() {
	}

	public static void setJreLocation(String jreLocation) {
		AzureServerWizardProperties.jreLocation = jreLocation;		
	}

	public static void setWebServerLocation(String webServerLocation) {
		AzureServerWizardProperties.webServerLocation = webServerLocation;
	}

	public static void setWebServerPort(String webServerPort) {
		AzureServerWizardProperties.webServerPort = webServerPort;
	}

	public static String getJreLocation() {
		return jreLocation;
	}

	public static String getWebServerLocation() {
		return webServerLocation;
	}

	public static String getWebServerPort() {
		return webServerPort;
	}
	
}
