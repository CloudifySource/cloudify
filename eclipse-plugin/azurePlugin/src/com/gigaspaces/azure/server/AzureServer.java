package com.gigaspaces.azure.server;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.gigaspaces.azure.AzurePlugin;

public class AzureServer extends ServerDelegate {
	private AzureConfiguration configuration = new AzureConfiguration(null);

	@Override
	public void saveConfiguration(IProgressMonitor monitor)
			throws CoreException {
		AzureConfiguration aConfig = configuration;
		if (aConfig == null)
			return;
		IFolder serverConfiguration = getServer().getServerConfiguration();
		aConfig.save(serverConfiguration, monitor);
		// Runtime.getRuntime().exec("emulatorTools/RunInEmulator.cmd");
		

		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(serverConfiguration.getLocation().toOSString()+ "/package.xml");
//		runner.setArguments("-Dmessage=Building -verbose");
		runner.run(monitor);
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyModules(IModule[] add, IModule[] remove,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public AzureConfiguration getServerConfiguration() throws CoreException {
		return getAzureConfiguration();
	}

	public AzureConfiguration getAzureConfiguration() throws CoreException {
		AzureConfiguration aConfig = null;
		// If configuration needs loading

		IFolder folder = getServer().getServerConfiguration();
		if (folder == null || !folder.exists()) {
			String path = null;
			if (folder != null) {
				path = folder.getFullPath().toOSString();
				IProject project = folder.getProject();
				if (project != null && project.exists() && !project.isOpen())
					throw new CoreException(new Status(IStatus.ERROR,
							AzurePlugin.PLUGIN_ID, 0, NLS.bind(
									"errorConfigurationProjectClosed", path,
									project.getName()), null));
			}
			throw new CoreException(new Status(IStatus.ERROR,
					AzurePlugin.PLUGIN_ID, 0, NLS.bind("errorNoConfiguration",
							path), null));
		}
		// If not yet loaded
		if (aConfig == null) {

			String id = getServer().getServerType().getId();
			aConfig = new AzureConfiguration(folder);
		}

		aConfig.load(folder, null);

		return aConfig;
	}

}
