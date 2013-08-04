package org.cloudifysource.esc.driver.provisioning;

import org.cloudifysource.domain.cloud.Cloud;
import org.openspaces.admin.Admin;

public class ComputeDriverConfiguration {

	private Cloud cloud;
	private String cloudTemplate;
	private boolean management;
	private String serviceName;
	private Admin admin;
	
	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public String getCloudTemplate() {
		return cloudTemplate;
	}

	public void setCloudTemplate(final String cloudTemplate) {
		this.cloudTemplate = cloudTemplate;
	}

	public boolean isManagement() {
		return management;
	}

	public void setManagement(final boolean management) {
		this.management = management;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

}
