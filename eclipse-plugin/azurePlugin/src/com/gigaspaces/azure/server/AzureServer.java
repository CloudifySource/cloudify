package com.gigaspaces.azure.server;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;

import pluginui.Activator;

import com.gigaspaces.azure.AzurePlugin;
import com.gigaspaces.azure.wizards.AzureServerWizardProperties;

public class AzureServer extends GenericServer {

	@Override
	public void saveConfiguration(IProgressMonitor monitor)
			throws CoreException {

		IFolder folder = getServer().getServerConfiguration();
		System.out.println("save config " + folder);
		try {

			if (!folder.exists()) {
				folder.create(true, true, monitor);
			}

			if (AzurePlugin.isTestMode) {
				IFolder testFolder = folder.getFolder("test");
				if (!testFolder.exists())
					testFolder.create(true, true, monitor);

				testFolder.touch(monitor);
				return;

			}

			if (folder.getFolder(
					AzureServerWizardProperties.getWorkerRoleName()).exists()) {
				// republish the wars
				IFolder webappsFolder = folder
						.getFolder(AzureServerWizardProperties
								.getWebAppsFolder());
				if (webappsFolder.exists())
					FileUtils.copyDirectory(new File("c:\\webapps"),
							webappsFolder.getLocation().toFile());
				return;
			}
			URL url = Activator.getDefault().getBundle()
					.getResource("resources");

			File resourcesDir = new File(FileLocator.resolve(url).toURI());

			FileUtils
					.copyDirectory(resourcesDir, folder.getLocation().toFile());


			folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					AzurePlugin.PLUGIN_ID, 0, NLS.bind(
							"errorCouldNotSaveConfiguration",
							new String[] { e.getMessage() }), e));
		}
	}

}
