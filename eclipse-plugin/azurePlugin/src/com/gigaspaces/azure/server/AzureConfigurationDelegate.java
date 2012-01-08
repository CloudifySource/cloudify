package com.gigaspaces.azure.server;

import java.io.IOException;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jst.server.generic.core.internal.Trace;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.gigaspaces.azure.AzurePlugin;

public class AzureConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate{

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		IServer server = ServerUtil.getServer(configuration);
		if (server == null) {
			throw new CoreException(new Status(Status.ERROR, AzurePlugin.PLUGIN_ID, "Launch configuration could not find server"));
		}
		
		IFolder folder = server.getServerConfiguration();
		System.out.println("running ant package.xml");
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(folder.getLocation().toOSString()
				+ "/package.xml");
		runner.run(monitor);

		AzureServerBehaviour srvDelegator = (AzureServerBehaviour) server.loadAdapter(ServerBehaviourDelegate.class, null);
		
		System.out.println("launch '" + mode +"'");
		
		srvDelegator.changeServerState(IServer.STATE_STARTING);
		
		if(AzurePlugin.isTestMode)
		{
			srvDelegator.changeServerState(IServer.STATE_STARTED);
			return;
		}
		String startEmulatorScript = "/emulatorTools/RunInEmulator.cmd";
	    try {
			Runtime.getRuntime().exec(server.getServerConfiguration().getLocation().toOSString()+startEmulatorScript);
			
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, AzurePlugin.PLUGIN_ID, "Failed to run the Azure Emulator - " + e.getMessage()));
		}
		
	    srvDelegator.monitorServerStartup();
	}

  
}
