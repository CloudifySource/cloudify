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
	private static String webServerLocation = "apache-tomcat-7.0.23";
	private static int webServerPort = 8080;
	private static String workerRoleName="WorkerRole1";
	private static String azurePackageName="WindowsAzurePackage.cspkg";

	public static String getAzurePackageName() {
		return azurePackageName;
	}

	public static void setAzurePackageName(String azurePackageName) {
		AzureServerWizardProperties.azurePackageName = azurePackageName;
	}

	public static String getWorkerRoleName() {
		return workerRoleName;
	}

	public static void setWorkerRoleName(String workerRoleName) {
		AzureServerWizardProperties.workerRoleName = workerRoleName;
	}

	protected AzureServerWizardProperties() {
	}

	public static void setJreLocation(String jreLocation) {
		AzureServerWizardProperties.jreLocation = jreLocation;		
	}

	public static void setWebServerLocation(String webServerLocation) {
		AzureServerWizardProperties.webServerLocation = webServerLocation;
	}

	public static void setWebServerPort(String webServerPort) {
		if(webServerPort == null)
			return;
		AzureServerWizardProperties.webServerPort = Integer.parseInt(webServerPort);
	}

	public static String getJreLocation() {
		return jreLocation;
	}

	public static String getWebServerLocation() {
		return webServerLocation;
	}

	public static int getWebServerPort() {
		return webServerPort;
	}

	public static String getWebAppsFolder()
	{
		return "deploy/" +azurePackageName + "/roles/" + workerRoleName + "/approot/" + webServerLocation + "/webapps";
	}
	public static void setDefaults() {
		// TODO Auto-generated method stub
		
	}
	
}
