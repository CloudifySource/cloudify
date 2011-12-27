package com.gigaspaces.azure.wizards;

import java.awt.Color;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.servertype.definition.Property;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.jst.server.generic.ui.internal.GenericServerComposite;
import org.eclipse.jst.server.generic.ui.internal.GenericServerCompositeDecorator;
import org.eclipse.jst.server.generic.ui.internal.GenericServerUIMessages;
import org.eclipse.jst.server.generic.ui.internal.GenericServerWizardFragment;
import org.eclipse.jst.server.generic.ui.internal.SWTUtil;
import org.eclipse.jst.server.generic.ui.internal.ServerDefinitionTypeAwareWizardFragment;
import org.eclipse.jst.server.generic.ui.internal.ServerTypeDefinitionServerDecorator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
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
		composite = new AzureServerWizardComposite(parent, handle); 
		return composite;
	}
	
	@Override
	public boolean hasComposite() {
		return true;
	}
	
	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		AzureServerWizardProperties.setJreLocation(composite.getJreLocation());
		AzureServerWizardProperties.setWebServerLocation(composite.getWebServerLocation());
		AzureServerWizardProperties.setWebServerPort(composite.getWebServerPort());
		super.performFinish(monitor);		
	}
	
	
}
