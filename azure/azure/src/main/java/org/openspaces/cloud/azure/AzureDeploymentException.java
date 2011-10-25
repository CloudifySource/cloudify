package org.openspaces.cloud.azure;

public class AzureDeploymentException extends Exception {

    private static final long serialVersionUID = 1L;

    public AzureDeploymentException() {
        super();
    }
    
    public AzureDeploymentException(Throwable e) {
        super(e);
    }
    
    public AzureDeploymentException(String msg, Throwable e) {
        super(msg, e);
    }
    
    public AzureDeploymentException(String msg) {
        super(msg);
    }
    
}
