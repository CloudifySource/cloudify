package com.gigaspaces.azure.wizards;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * Azure server wizard fragment.
 * 
 * @author idan
 *
 */
public class AzureServerWizardFragment extends WizardFragment {

	private AzureServerWizardComposite composite;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		return  new AzureServerWizardComposite(parent, handle); 
	}
	
	@Override
	public boolean hasComposite() {
		return true;
	}
	


	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		if(composite == null)
		{
			AzureServerWizardProperties.setDefaults();
			return;
		}
		AzureServerWizardProperties.setJreLocation(composite.getJreLocation());
	//	AzureServerWizardProperties.setWebServerLocation(composite.getWebServerLocation());
		AzureServerWizardProperties.setWebServerPort(composite.getWebServerPort());
		super.performFinish(monitor);		
	
	}

}
