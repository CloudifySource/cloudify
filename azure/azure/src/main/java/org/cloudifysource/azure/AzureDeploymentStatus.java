package org.cloudifysource.azure;

//CR: Where did you get the strings from ? according to http://msdn.microsoft.com/en-us/library/ee460804.aspx it is different
//CR: Document which Azure API version this enum conforms to
public enum AzureDeploymentStatus {
    Running("Running"), 
    Suspended("Suspended"), 
    RunningTransitioning("RunningTransitioning"),
    SuspendedTransitioning("SuspendedTransitioning"),
    Starting("Starting"),
    Suspending("Suspending"),
    Deploying("Deploying"), 
    Deleting("Deleting"),
    
    // Not actual status, possible output of azureconfig
    NotFound("NotFound"), 
    InternalServerError("Intenal Server Error");
    
    private String status;
    
    private AzureDeploymentStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public static AzureDeploymentStatus fromString(String status) {
        for (AzureDeploymentStatus deploymentStatus : values()) {
            if (deploymentStatus.getStatus().equals(status)) {
                return deploymentStatus;
            }
        }
        throw new IllegalArgumentException("No such status: " + status);
    }
}
