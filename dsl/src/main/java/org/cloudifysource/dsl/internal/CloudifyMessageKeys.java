package org.cloudifysource.dsl.internal;

public enum CloudifyMessageKeys {
	
	OPERATION_SUCCESSFULL("operation_successfull"),
	
	ATTRIBUTE_DELETED_SUCCESSFULLY("deleted_instance_attribute"),
	
	API_VERSION_MISMATCH("version_mismatch"),
	
	SERVICE_WAIT_TIMEOUT("service_wait_timeout"),
	
	APPLICATION_WAIT_TIMEOUT("application_wait_timeout");
		
	private final String name;
	
	CloudifyMessageKeys(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
