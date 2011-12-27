package com.gigaspaces.azure.deploy.files;


public class AzureServiceDefinition {

    private final String serviceName;
    private final int externalPort;
    private final int internalPort;
    private final boolean inputEndpointDefined;
    private final int numberOfInstances;
    
    public AzureServiceDefinition(String serviceName, int numberOfInstances, int externalPort, int internalPort) {
    	this(serviceName, numberOfInstances, externalPort, internalPort, true);
    }
    
    public AzureServiceDefinition(String serviceName, int numberOfInstances) {
        this(serviceName, numberOfInstances, 0, 0, false);
    }
    
    private AzureServiceDefinition(String serviceName, int numberOfInstances, int externalPort, int internalPort, boolean inputEndpointDefined) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }
        if (!isValidPortNumber(internalPort) || !isValidPortNumber(externalPort)) {
            throw new IllegalArgumentException("Illegal port numbers: (" + internalPort + "," + externalPort + ")");
        }
        this.serviceName = serviceName;
        this.externalPort = externalPort;
        this.internalPort = internalPort;
        this.numberOfInstances = numberOfInstances;
        this.inputEndpointDefined = inputEndpointDefined;
    }
    
    public boolean isInputEndpointDefined() {
        return inputEndpointDefined;
    }

    public String getServiceName() {
        return serviceName;
    }
    
    public int getExternalPort() {
        return externalPort;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }
    
    private static boolean isValidPortNumber(int portNumber) {
        return portNumber >= 0 && portNumber <= 65535;
    }
    
    public static String[] extractServiceNames(AzureServiceDefinition[] serviceDefinitions) {
        String[] serviceNames = new String[serviceDefinitions.length];
        for (int i=0; i<serviceNames.length; i++) {
            serviceNames[i] = serviceDefinitions[i].getServiceName();
        }
        return serviceNames;
    }
    
}
