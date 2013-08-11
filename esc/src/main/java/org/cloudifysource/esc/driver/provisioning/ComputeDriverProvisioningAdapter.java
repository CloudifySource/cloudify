package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;

@SuppressWarnings("deprecation")
public class ComputeDriverProvisioningAdapter extends BaseComputeDriver {

	public static BaseComputeDriver create(final Object driverInstance) {
		if (driverInstance instanceof BaseComputeDriver) {
			return (BaseComputeDriver) driverInstance;
		}
		if (driverInstance instanceof ProvisioningDriver) {
			return new ComputeDriverProvisioningAdapter((ProvisioningDriver) driverInstance);
		}
		throw new IllegalArgumentException("Driver class is does not extend " + BaseComputeDriver.class.getName()
				+ " or implement " + ProvisioningDriver.class.getName());

	}

	private final ProvisioningDriver provisioningDriver;

	private ComputeDriverProvisioningAdapter(final ProvisioningDriver provisioningDriver) {
		this.provisioningDriver = provisioningDriver;
	}

	@Override
	public void close() {
		provisioningDriver.close();
	}

	@Override
	public void addListener(final ProvisioningDriverListener listener) {
		provisioningDriver.addListener(listener);
	}

	@Override
	public String getCloudName() {
		return provisioningDriver.getCloudName();
	}

	@Override
	public Object getComputeContext() {

		return provisioningDriver.getComputeContext();
	}

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		provisioningDriver.onServiceUninstalled(duration, unit);
	}

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		provisioningDriver.setConfig(configuration.getCloud(), configuration.getCloudTemplate(),
				configuration.isManagement(), configuration.getServiceName());
	}

	@Override
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException {
		ProvisioningContextAccess.setCurrentProvisioingContext(context);
		try {
			return super.startMachine(context, duration, unit);
		} finally {
			// clear thread local.
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		return provisioningDriver.stopMachine(machineIp, duration, unit);
	}

	@Override
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		ProvisioningContextAccess.setCurrentManagementProvisioingContext(context);
		try {
			return provisioningDriver.startManagementMachines(duration, unit);
		} finally {
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

	@Override
	public void setProvisioningDriverClassContext(final ProvisioningDriverClassContext context) {
		if (this.provisioningDriver instanceof ProvisioningDriverClassContextAware) {
			((ProvisioningDriverClassContextAware) this.provisioningDriver).setProvisioningDriverClassContext(context);
		} else {
			super.setProvisioningDriverClassContext(context);
		}
	}

	@Override
	public void setCustomDataFile(final File customDataFile) {
		if (this.provisioningDriver instanceof CustomServiceDataAware) {
			((CustomServiceDataAware) this.provisioningDriver).setCustomDataFile(customDataFile);
		} else {
			super.setCustomDataFile(customDataFile);
		}
	}

	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		if (this.provisioningDriver instanceof ManagementLocator) {
			return ((ManagementLocator) this.provisioningDriver).getExistingManagementServers();
		} else {
			return super.getExistingManagementServers();
		}

	}

	@Override
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		if (this.provisioningDriver instanceof ManagementLocator) {
			return ((ManagementLocator) this.provisioningDriver).getExistingManagementServers(controllers);
		} else {
			return super.getExistingManagementServers(controllers);
		}

	}

	@Override
	public void validateCloudConfiguration(final ValidationContext validationContext) 
			throws CloudProvisioningException {
		if (this.provisioningDriver instanceof ProvisioningDriverBootstrapValidation) {
			((ProvisioningDriverBootstrapValidation) this.provisioningDriver)
					.validateCloudConfiguration(validationContext);
		} else {
			super.validateCloudConfiguration(validationContext);
		}
	}

}
