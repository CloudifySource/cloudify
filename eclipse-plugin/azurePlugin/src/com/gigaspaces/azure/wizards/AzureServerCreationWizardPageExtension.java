package com.gigaspaces.azure.wizards;

import java.beans.PropertyChangeEvent;

import org.eclipse.jst.server.generic.ui.internal.SWTUtil;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.ui.wizard.ServerCreationWizardPageExtension;

public class AzureServerCreationWizardPageExtension extends
		ServerCreationWizardPageExtension {

	@Override
	public void createControl(UI_POSITION position, Composite parent) {

		// add Azure Project file browser at the bottom of the wizard page
		if (position == ServerCreationWizardPageExtension.UI_POSITION.BOTTOM) {
			final Text azureProjectLocation = SWTUtil.createLabeledPath(
					"Azure Project:", "", parent);
			azureProjectLocation.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					System.out.println(azureProjectLocation.getText());
				}
			});
		}

	}

	@Override
	public void handlePropertyChanged(PropertyChangeEvent event) {
		// TODO Auto-generated method stub

	}

}
