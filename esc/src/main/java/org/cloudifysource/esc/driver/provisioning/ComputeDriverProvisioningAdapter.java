package org.cloudifysource.esc.driver.provisioning;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException, TimeoutException,
			CloudProvisioningException {
		// TODO Auto-generated method stub
		provisioningDriver.onServiceUninstalled(duration, unit);
	}

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		// TODO Auto-generated method stub
		provisioningDriver.setConfig(configuration.getCloud(), configuration.getCloudTemplate(),
				configuration.isManagement(), configuration.getServiceName());
	}

	@Override
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException {
		// TODO Auto-generated method stub
		ProvisioningContextAccess.setCurrentProvisioingContext(context);
		try {
			return super.startMachine(context, duration, unit);
		} finally {
			// clear thread local.
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		// TODO Auto-generated method stub
		return provisioningDriver.stopMachine(machineIp, duration, unit);
	}

	@Override
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		// TODO Auto-generated method stub
		ProvisioningContextAccess.setCurrentManagementProvisioingContext(context);
		try {
			return provisioningDriver.startManagementMachines(duration, unit);
		} finally {
			ProvisioningContextAccess.setCurrentProvisioingContext(null);
		}
	}

}
