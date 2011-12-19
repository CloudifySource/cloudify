package com.gigaspaces.azure.wizards;

import java.awt.Color;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.servertype.definition.Property;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.jst.server.generic.ui.internal.GenericServerComposite;
import org.eclipse.jst.server.generic.ui.internal.GenericServerCompositeDecorator;
import org.eclipse.jst.server.generic.ui.internal.GenericServerUIMessages;
import org.eclipse.jst.server.generic.ui.internal.GenericServerWizardFragment;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

public class AzureServerWizardFragment extends GenericServerWizardFragment {

	@Override
	public void createContent(Composite parent, IWizardHandle handle) {
		super.createContent(parent, handle);
	
        GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gridData.horizontalSpan = 1;
        
        GridData fillData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fillData.heightHint = 100;
        
        // Servers
        final Group serversGroup = new Group(parent, SWT.NONE);
        serversGroup.setLayout(new GridLayout(1, false));
        serversGroup.setLayoutData(gridData);
        serversGroup.setText("Web Servers");
        
        final Tree serversTree = new Tree(serversGroup, SWT.BORDER);
        serversTree.setLayoutData(fillData);
		final TreeItem tomcatRootItem = new TreeItem(serversTree, SWT.NONE);
		tomcatRootItem.setText("Apache Tomcat");
		new TreeItem(tomcatRootItem, SWT.NONE).setText("Tomcat v5.0 Server");
		new TreeItem(tomcatRootItem, SWT.NONE).setText("Tomcat v5.5 Server");
		new TreeItem(tomcatRootItem, SWT.NONE).setText("Tomcat v6.0 Server");
		new TreeItem(tomcatRootItem, SWT.NONE).setText("Tomcat v7.0 Server");
		final TreeItem jettyRootItem = new TreeItem(serversTree, SWT.NONE);
		jettyRootItem.setText("Jetty");
		new TreeItem(jettyRootItem, SWT.NONE).setText("Jetty WebServer 8.1.0");
				
		// Roles
		final Group rolesGroup = new Group(parent, SWT.NONE);
		rolesGroup.setText("Roles");
        rolesGroup.setLayout(new GridLayout(1, false));
        rolesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		final Table rolesTable = new Table(rolesGroup, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		rolesTable.setLayoutData(fillData);
		rolesTable.setHeaderVisible(true);
		rolesTable.setLinesVisible(true);
		
		new TableColumn(rolesTable, SWT.NONE).setText("Name");
		new TableColumn(rolesTable, SWT.NONE).setText("VM Size");
		new TableColumn(rolesTable, SWT.NONE).setText("Instances");
		
		new TableItem(rolesTable, SWT.NONE, 0).setText(new String[] { "WorkerRole1", "Small", "1" });
		
		rolesTable.getColumn(0).pack();
		rolesTable.getColumn(1).pack();
		rolesTable.getColumn(2).pack();	
	}
	
	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		
		// It is possible to get the values from the form by saving the parent composite when entering the createContent method
		// and then getting the values from its instance here.
	}
	
	
}
