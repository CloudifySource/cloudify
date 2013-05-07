package org.cloudifysource.esc.installer.remoteExec;

import org.apache.commons.lang.StringUtils;


/**
 * Bootstrap script error codes and messages.
 * @author noak
 *
 * @since 2.6.0
 */
public enum BootstrapScriptErrors {

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
	 * Custom error.
	 */
	CUSTOM(255, "");

	private final int errorCode;
	private final String errorMessage;

	BootstrapScriptErrors(final int errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

    BootstrapScriptErrors(final String errorMessage) {
        this.errorCode = 500;
        this.errorMessage = errorMessage;
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
