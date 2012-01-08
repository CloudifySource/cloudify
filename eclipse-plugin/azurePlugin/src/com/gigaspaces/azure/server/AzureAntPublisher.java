package com.gigaspaces.azure.server;

import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jst.server.generic.core.internal.publishers.AntPublisher;
import org.eclipse.wst.server.core.IServer;

import com.gigaspaces.azure.AzurePlugin;
import com.gigaspaces.azure.wizards.AzureServerWizardProperties;

public class AzureAntPublisher extends
		org.eclipse.jst.server.generic.core.internal.publishers.AntPublisher {
	public static final String PUBLISHER_ID = "org.eclipse.jst.server.generic.azure.antpublisher"; //$NON-NLS-1$

	protected void setupAntLaunchConfiguration(
			ILaunchConfigurationWorkingCopy wc) {
		try {
			// TODO use the actual property names
			Map antProperties = (Map) wc.getAttributes().get(
					"org.eclipse.ui.externaltools.ATTR_ANT_PROPERTIES");

			IFolder serverConfiguration = getServer().getServer()
					.getServerConfiguration();

			if (antProperties != null) {

				String webapps = "c:\\webapps";

				if (getServer().getServer().getServerState() == IServer.STATE_STARTED) {
					IFolder webappsFolder = serverConfiguration
							.getFolder(AzureServerWizardProperties
									.getWebAppsFolder());
					webapps = webappsFolder.getLocation().toOSString();
				}
				antProperties
						.put(AntPublisher.PROP_SERVER_PUBLISH_DIR, webapps);
			}
			System.out.println("publish " + antProperties);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}