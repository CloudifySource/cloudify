package org.cloudifysource.esc.driver.provisioning;

public class ProvisioningContextAccessImpl implements ProvisioningContextAccess {

	private ProvisioningContextAccessImpl () {
		
	}
	
	private static ThreadLocal<ProvisioningContext> contextHolder = new ThreadLocal<ProvisioningContext>();
	
	@Override
	public ProvisioningContext getProvisioiningContext() {
		return contextHolder.get();
	}
	
	public static void setCurrentProvisioingContext(final ProvisioningContext context) {
		contextHolder.set(context);
	}

	
}
