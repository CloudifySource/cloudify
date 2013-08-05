package org.cloudifysource.rest.validators;

import org.cloudifysource.domain.cloud.Cloud;

/**
 * An un-install application validation context containing all necessary
 * validation parameters required for validation.
 *
 * @author adaml
 *
 */
public class UninstallApplicationValidationContext {

	private Cloud cloud;

	private String applicationName;

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}
}
