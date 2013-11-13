package org.cloudifysource.esc.installer.remoteExec;

import org.apache.commons.lang.StringUtils;


/**
 * Bootstrap script error codes and messages.
 * @author noak
 *
 * @since 2.6.0
 */
public enum BootstrapScriptErrors {

    /**
     * Script terminated unexpectedly.
     */
    UNEXPECTED_TERMINATION("Script terminated unexpectedly"),

	/**
	 * Cloudify environment file not found.
	 */
	CLOUDIFY_ENV_FILE_MISSING(100, "Cloudify environment file not found! Bootstrapping cannot proceed!"),
	
	/**
	 * Failed downloading Java installation.
	 */
	JAVA_DOWNLOAD_FAILED(101, "Failed downloading Java installation"),
	
	/**
	 * Failed removing old java installation directory.
	 */
	JAVA_DIR_REMOVE_FAILED(102, "Failed removing old java installation directory"),
	
	/**
	 * Failed moving JDK installation.
	 */
	MOVING_JDK_FAILED(103, "Failed moving JDK installation"),
	
	/**
	 * Failed downloading Cloudify installation.
	 */
	CLOUDIFY_DOWNLOAD_FAILED(104, "Failed downloading cloudify installation"),
	
	/**
	 * Failed downloading Cloudify overrides.
	 */
	CLOUDIFY_OVERRIDES_DOWNLOAD_FAILED(105, "Failed downloading cloudify overrides"),
	
	/**
	 * Failed removing old Gigaspaces directory.
	 */
	GIGASPACES_DIR_REMOVE_FAILED(106, "Failed removing old gigaspaces directory"),
	
	/**
	 * Failed creating Gigaspaces directory.
	 */
	GIGASPACES_DIR_CREATION_FAILED(107, "Failed creating gigaspaces directory"),
	
	/**
	 * Failed extracting Cloudify installation.
	 */
	CLOUDIFY_EXTRACTION_FAILED(108, "Failed extracting cloudify installation"),
	
	/**
	 * Failed changing permissions in Cloudify installation.
	 */
	CLOUDIFY_CHMOD_FAILED(109, "Failed changing permissions in cloudify installation"),
	
	/**
	 * Failed moving Cloudify installation.
	 */
	MOVING_CLOUDIFY_DIR_FAILED(110, "Failed moving cloudify installation"),
	
	/**
	 * Failed extracting cloudify overrides.
	 */
	CLOUDIFY_OVERRIDES_EXTRACTION_FAILED(111, "Failed extracting cloudify overrides"),
	
	/**
	 * Failed changing directory to bin directory.
	 */
	FAILED_CHANGING_TO_BIN_DIR(112, "Failed changing directory to bin directory"),
	
	/**
	 * Failed updating setenv.sh.
	 */
	UPDATING_SETENV_FILE_FAILED(113, "Failed updating setenv.sh"),
	
	/**
	 * Failed to remove nohup.out, it might be used by another process.
	 */
	FAILED_REMOVING_NOHUP_OUT(114, "Failed to remove nohup.out, it might be used by another process"),
	
	/**
	 * Current user is not a sudoer, or requires a password for sudo.
	 */
	USER_NOT_SUDOER(115, "Current user is not a sudoer, or requires a password for sudo"),
	
	/**
	 * Could not find sudoers file at expected location (/etc/sudoers).
	 */
	SUDOERS_FILE_NOT_FOUND(116, "Could not find sudoers file at expected location (/etc/sudoers)"),
	
	/**
	 * Failed to edit sudoers file to disable requiretty directive.
	 */
	SUDOERS_FILE_EDIT_FAILED(117, "Failed to edit sudoers file to disable requiretty directive"),
	
	/**
	 * Failed changing directory to cli directory.
	 */
	FAILED_CHANGING_TO_CLI_DIR(118, "Failed changing directory to cli directory"),
	
	/**
	 * Host name and address validation aborted, host could not be resolved.
	 */
	HOST_VALIDATION_ABORTED_UNKNOWN_HOST(119, "Host name and address validation aborted, host could not be resolved."),
	
	/**
	 * Host name and address validation aborted, host could not be resolved.
	 */
	HOST_VALIDATION_ABORTED_NO_PERMISSION(120, "Host validation aborted, a security manager exists and permission to "
			+ "perform the operation is denied."),
			
	/**
	 * NIC validation aborted, host could not be resolved.
	 */
	NIC_VALIDATION_ABORTED_UNKNOWN_HOST(121, "NIC validation aborted, host could not be resolved."),
	
	/**
	 * NIC validation aborted, I/O error occurred when creating the socket or connecting.
	 */
	NIC_VALIDATION_ABORTED_IO_ERROR(122, "NIC validation aborted, I/O error occurred when creating the socket or "
			+ "connecting."),
	
	/**
	 * NIC validation aborted, a security manager exists and permission to perform the operation is denied.
	 */
	NIC_VALIDATION_ABORTED_NO_PERMISSION(123, "NIC validation aborted, a security manager exists and permission to "
			+ "perform the operation is denied."),
	
	/**
	 * Port validation aborted, host could not be resolved.
	 */
	PORT_VALIDATION_ABORTED_UNKNOWN_HOST(124, "Port validation aborted, host could not be resolved."),
	
	/**
	 * Port validation aborted, I/O error occurred when creating the socket or connecting.
	 */
	PORT_VALIDATION_ABORTED_IO_ERROR(125, "Port validation aborted, I/O error occurred when creating the socket or "
			+ "connecting."),
	
	/**
	 * Port validation aborted, a security manager exists and permission to perform the operation is denied.
	 */
	PORT_VALIDATION_ABORTED_NO_PERMISSION(126, "Port validation aborted, a security manager exists and permission to "
			+ "perform the operation is denied."),
	
	/**
	 * The lookup service connection validation was aborted, host could not be resolved.
	 */
	LUS_CONNECTION_VALIDATION_ABORTED_UNKNOWN_HOST(127, "The lookup service connection validation was aborted, host "
			+ "could not be resolved."),
	
	/**
	 * The lookup service connection validation was aborted, I/O error occurred when creating the socket or connecting.
	 */
	LUS_CONNECTION_VALIDATION_ABORTED_IO_ERROR(128, " The lookup service connection validation was aborted, I/O error "
			+ "occurred when creating the socket or connecting."),
	
	/**
	 * The lookup service connection validation aborted, a security manager exists and permission to perform the 
	 * operation is denied.
	 */
	LUS_CONNECTION_VALIDATION_ABORTED_NO_PERMISSION(129, "The lookup service connection validation aborted, a security "
			+ "manager exists and permission to perform the operation is denied."),
			
	/**
	 * Unexpected validation error occurred.
	 */
	CUSTOM_VALIDATION_ERROR(130, "Cloudify validation failed."),
	
	/**
	 * Post bootstrap validations failed to find a running agent.
	 */
	POST_BOOTSTRAP_NO_AGENT_FOUND(131, "Failed to find a running agent after bootstrap completed."),
	
	/**
	 * Some management components are not available after bootstrap completed (LUS/GSM/ESM).
	 */
	POST_BOOTSTRAP_MISSING_MGMT_COMPONENT(132, "Some management components (LUS/ESM/GSM) are not available after "
			+ "bootstrap completed. Please review the logs for more details."),
			
	/**
	 * Failed to find a required management service (space, web-UI or Rest) after bootstrap completed.
	 */
	POST_BOOTSTRAP_MISSING_MGMT_SERVICE(133, "Failed to find a required management service (space, web-UI or Rest) "
			+ "after bootstrap completed. Please review the logs for more details."),

    /**
     * Failed to clean the script home directory from the gigaspaces tar file.
     */
    FAILED_DELETING_GIGASPACES_TAR(134, "Failed deleting gigaspaces.tar.gz from home directory"),

    /**
     * Failed to clean the script home directory from the gigaspaces_overrides tar file.
     */
    FAILED_DELETING_GIGASPACES_OVERRIDES_TAR(135, "Failed deleting gigaspaces_overrides.tar.gz from home directory"),

    /**
     * Failed to clean the script home directory from the java bin.
     */
    FAILED_DELETING_JAVA_BIN(136, "Failed deleting java.bin from home directory"),


    /**
     * IMPORTANT NOTE: If the error code is larger than 200, you must edit bootstrap-management.sh or it
     * would be re-thrown as code 255.
     */
	
	/**
	 * Custom error.
	 */
	CUSTOM_ERROR(255, "");

	private final int errorCode;
	private final String errorMessage;

	BootstrapScriptErrors(final int errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

    BootstrapScriptErrors(final String errorMessage) {
        this(500, errorMessage);
    }

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Gets the message text by the error code.
	 * @param errorCode The code of the relevant error
	 * @return The error message
	 */
    public static String getMessageByErrorCode(final int errorCode) {
    	
        String errorMessage = "";
        for (BootstrapScriptErrors bootstrapError : values()) {
            if (errorCode == (bootstrapError.getErrorCode())) {
            	errorMessage = bootstrapError.getErrorMessage();
            }
        }
        
        if (StringUtils.isBlank(errorMessage)) {
        	errorMessage = UNEXPECTED_TERMINATION.getErrorMessage();
        }
        
        return errorMessage;
    }

}
