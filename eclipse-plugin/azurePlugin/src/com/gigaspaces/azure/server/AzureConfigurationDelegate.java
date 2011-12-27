package com.gigaspaces.azure.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

public class AzureConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate{

	@Override
	public void launch(ILaunchConfiguration arg0, String arg1, ILaunch arg2,
			IProgressMonitor arg3) throws CoreException {

		System.out.println("launch ");
	}

}
