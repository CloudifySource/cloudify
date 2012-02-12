package com.gigaspaces.azure.wizards;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.gigaspaces.azure.server.AzureRole;

/**
 * Statically holds windows azure server creation wizard properties.
 * The properties are updated after the user presses the wizard's "finish" button.
 * 
 * @author idan
 *
 */
public class AzureServerWizardProperties {

	private static String jreLocation;
	private static volatile String webServerLocation ;
	private static int webServerPort = 8080;
	private static String workerRoleName;
	private static String projectName;
	private static AzureRole role;
	

	private static String azurePackageName="WindowsAzurePackage.cspkg";
	private static IServer server;

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

	public static String getProjectName() {
		return projectName;
	}

	public static void setProjectName(String projectName) {
		AzureServerWizardProperties.projectName = projectName;
	}
	
	
	public static AzureRole getRole() {
		return role;
	}

	public static void setRole(AzureRole role) {
		AzureServerWizardProperties.role = role;
	}

	public static void setWebServer(IServer server) {
		AzureServerWizardProperties.setServer(server);
	}

	public static IServer getServer() {
		return server;
	}

	public static void setServer(IServer server) {
		AzureServerWizardProperties.server = server;
	}

}
