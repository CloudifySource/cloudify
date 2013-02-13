package org.cloudifysource.esc.driver.provisioning.storage;

/**
 * Dedicated exception for storage provisioning errors.
 * @author elip
 *
 */
public class StorageProvisioningException extends Exception {
	
	public StorageProvisioningException(final String message) {
		super(message);
	}
	
	public StorageProvisioningException(final Exception e) {
		super(e);
	}
	
	public StorageProvisioningException(final String message, final Exception e) {
		super(message, e);
	}
	
	public StorageProvisioningException() {
		super();
	}
	
}
