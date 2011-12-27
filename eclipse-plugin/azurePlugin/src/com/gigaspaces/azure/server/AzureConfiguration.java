package com.gigaspaces.azure.server;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.internal.registry.osgi.Activator;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.gigaspaces.azure.AzurePlugin;

public class AzureConfiguration {

	private final IFolder configPath;

	public AzureConfiguration(IFolder path) {
		super();
		this.configPath = path;
	}

	/**
	 * Save the information held by this object to the given directory.
	 * 
	 * @param folder
	 *            a folder
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException
	 */
	public void save(IFolder folder, IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);

			if (!folder.exists())
				folder.create(true, true,
						ProgressUtil.getSubMonitorFor(monitor, 100));
			else
				monitor.worked(100);

			URL url = Activator.getContext().getBundle(AzurePlugin.PLUGIN_ID).getResource("resources");
//			URL url = new URL("platform:/plugin/" + AzurePlugin.PLUGIN_ID
//					+ "/resources");
			File resourcesDir = new File(FileLocator.resolve(url).toURI());

			FileUtils
					.copyDirectory(resourcesDir, folder.getLocation().toFile());

			if (monitor.isCanceled())
				return;
			monitor.done();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					AzurePlugin.PLUGIN_ID, 0, NLS.bind(
							"errorCouldNotSaveConfiguration",
							new String[] { e.getLocalizedMessage() }), e));
		}
	}

	public void load(IFolder folder, Object object) {

	}
}
