package com.gigaspaces.azure.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericPublisher;
import org.eclipse.jst.server.generic.core.internal.GenericServerBehaviour;
import org.eclipse.jst.server.generic.core.internal.GenericServerCoreMessages;
import org.eclipse.jst.server.generic.core.internal.PublishManager;
import org.eclipse.jst.server.generic.core.internal.ServerTypeDefinitionUtil;
import org.eclipse.jst.server.generic.core.internal.Trace;
import org.eclipse.jst.server.generic.core.internal.publishers.AntPublisher;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.gigaspaces.azure.AzurePlugin;
import com.gigaspaces.azure.wizards.AzureServerWizardProperties;

public class AzureServerBehaviour extends GenericServerBehaviour {

	@Override
	protected void shutdown(int state) {

		System.out.println("Stopping Server"); //$NON-NLS-1$
		if (state != IServer.STATE_STOPPED)
			setServerState(IServer.STATE_STOPPING);

		if(AzurePlugin.isTestMode)
		{
			setServerState(IServer.STATE_STOPPED);
			return;
		}
		String stopEmulatorScript = "/emulatorTools/ResetEmulator.cmd";
		try {
			Runtime.getRuntime().exec(
					getServer().getServerConfiguration().getLocation()
							.toOSString()
							+ stopEmulatorScript);
		} catch (IOException e) {
			return;
		}
		finally
		{
			setServerState(IServer.STATE_STOPPED);
			System.out.println("Server stopped"); //$NON-NLS-1$
		}

	}

	public void changeServerState(int state) {
		setServerState(state);
	}

	public void setServerStarted() {
		setServerState(IServer.STATE_STARTED);
	}

	public void monitorServerStartup() {
		startPingThread();
	}

}
