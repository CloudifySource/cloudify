package org.cloudifysource.esc.driver.provisioning;

public class ProvisioningContextAccess {

	private static ThreadLocal<ProvisioningContext> contextHolder = new ThreadLocal<ProvisioningContext>();
	private static ThreadLocal<ManagementProvisioningContext> mgtContextHolder =
			new ThreadLocal<ManagementProvisioningContext>();

	private ProvisioningContextAccess() {

	}

	public ProvisioningContext getProvisioiningContext() {
		return contextHolder.get();
	}

	public static void setCurrentProvisioingContext(final ProvisioningContext context) {
		contextHolder.set(context);
	}

	public ManagementProvisioningContext getManagementProvisioiningContext() {
		return mgtContextHolder.get();
	}

	public static void setCurrentManagementProvisioingContext(final ManagementProvisioningContext context) {
		mgtContextHolder.set(context);
	}

}
