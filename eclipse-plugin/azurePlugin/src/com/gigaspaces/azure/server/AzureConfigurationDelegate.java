package com.gigaspaces.azure.server;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.gigaspaces.azure.AzurePlugin;
import com.gigaspaces.azure.wizards.AzureServerWizardProperties;

public class AzureConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate{

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		IServer server = AzureServerWizardProperties.getServer();
		if (server == null) {
			throw new CoreException(new Status(Status.ERROR, AzurePlugin.PLUGIN_ID, "Launch configuration could not find server"));
		}
		
//		IFolder folder = server.getServerConfiguration();
		//zip the webserver
		AzureRole role = AzureServerWizardProperties.getRole();
		AzureProject project = role.getProject();
		//project.getProject().open(null);

		IFolder zipFile = project.getProject().getFolder(role.getRole() +"/approot/" + "tomcat7.zip");
			
		String webserverLocation = server.getRuntime().getLocation().toOSString();
		System.out.println("zipping the webserver: "  + webserverLocation + " --> " + zipFile);
		AzureProjectTools.createZipFile(zipFile.getLocation().toOSString(), webserverLocation);
		
		IFolder jdkZipFile = project.getProject().getFolder(role.getRole() +"/approot/" + "jdk.zip");
		
		IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
			
		File jdkLocation = defaultVMInstall.getInstallLocation();
		System.out.println("zipping the zdk: "  + jdkLocation  + " --> " + jdkZipFile);
		AzureProjectTools.createZipFile(jdkZipFile.getLocation().toOSString(), jdkLocation.getPath());
		
//		project.getProject().refreshLocal(IResource.DEPTH_INFINITE,monitor);
		
		//update the startup.cmd script
		System.out.println("updating the startup.cmd");
		try {
			File startupFile = new File(project.getProject().getFolder(role.getRole() +"/approot/startup.cmd").getLocation()
					.toOSString());
			FileUtils.copyFile(
					new File(project.getProject().getFile("samples/startupApacheTomcat7.txt").getLocation()
							.toOSString()),
					startupFile);
			
			replace(startupFile,"SET SERVER_DIR_NAME=(.*)","SET SERVER_DIR_NAME=" + new File(webserverLocation).getName());
			
		} catch (IOException e1) {
			throw new CoreException(new Status(Status.ERROR, AzurePlugin.PLUGIN_ID, e1.getMessage()));
		}

//		project.getProject().refreshLocal(IResource.DEPTH_INFINITE,monitor);
		
		System.out.println("running ant package.xml");
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(project.getProject().getLocation().toOSString()
				+ "/package.xml");
		runner.run(monitor);

		IServer azureServer = ServerUtil.getServer(configuration);
		AzureServerBehaviour srvDelegator = (AzureServerBehaviour) azureServer.loadAdapter(ServerBehaviourDelegate.class, null);
		
		System.out.println("launch '" + mode +"'");
		
		srvDelegator.changeServerState(IServer.STATE_STARTING);
		
		if(AzurePlugin.isTestMode)
		{
			srvDelegator.changeServerState(IServer.STATE_STARTED);
			return;
		}
		String startEmulatorScript = "/emulatorTools/RunInEmulator.cmd";
	    try {
			Runtime.getRuntime().exec(project.getProject().getLocation().toOSString()+startEmulatorScript);
			
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, AzurePlugin.PLUGIN_ID, "Failed to run the Azure Emulator - " + e.getMessage()));
		}
		
	    srvDelegator.monitorServerStartup();
	}

	private void replace(File startupFile, String regex, String replacement) throws IOException {
		System.out.println("replacing in " + startupFile + "," +regex + ","  +replacement);
		
		String fileToString = FileUtils.readFileToString(startupFile);
		
		String replacedString = fileToString.replaceFirst(regex, replacement);
		
		FileUtils.writeStringToFile(startupFile, replacedString);
	}

  
}
