package org.cloudifysource.azure.files;

import java.io.File;

import org.cloudifysource.azure.files.xml.XMLElementAttribute;


//CR: Add link to schema http://msdn.microsoft.com/en-us/library/ee758711.aspx
public class AzureDeploymentDefinitionFile extends AbstractAzureDeploymentFile {

    public AzureDeploymentDefinitionFile(File file)
            throws XMLXPathEditorException {
        super(file);
    }

    public void setDeploymentName(String deploymentName) throws XMLXPathEditorException {
        getEditor().setNodeAttribute("/ServiceDefinition", "name", deploymentName);
    }
    
    public void setServices(AzureServiceDefinition[] serviceDefinitions) throws XMLXPathEditorException {
        String[] services = AzureServiceDefinition.extractServiceNames(serviceDefinitions);
        getEditor().findNodeDuplicateByAttribute("/ServiceDefinition/WorkerRole[@name='internal']", services, "name");
        
        for (AzureServiceDefinition serviceDefinition : serviceDefinitions) {
            if (serviceDefinition.isInputEndpointDefined()) {
                try {
                    String rawLrmiMinPort = getEditor().getNodeValue(getMinFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI"));
                    String rawLrmiMaxPort = getEditor().getNodeValue(getMaxFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI"));
                    int lrmiMinPort = Integer.parseInt(rawLrmiMinPort);
                    int lrmiMaxPort = Integer.parseInt(rawLrmiMaxPort);
                    
                    // TODO: assuming lrmiMinPort < lrmiMaxPort, what should be done otherwise?
                    if (serviceDefinition.getInternalPort() == lrmiMinPort) {
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI"), 
                                "min", String.valueOf(lrmiMinPort + 1));
                    } else if (serviceDefinition.getInternalPort() == lrmiMaxPort)
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI"), 
                                "max", String.valueOf(lrmiMaxPort - 1));                   
                    if (serviceDefinition.getInternalPort() > lrmiMinPort && serviceDefinition.getInternalPort() < lrmiMaxPort) {
                        getEditor().findNodeDuplicateByAttribute(
                                getInternalEndpointXPath(serviceDefinition.getServiceName(), "LRMI"),
                                new String[] { "LRMI1", "LRMI2" }, "name");
                        
                        int lowLrmiMinPort = lrmiMinPort;
                        int lowLrmiMaxPort = serviceDefinition.getInternalPort() - 1;
                        
                        int highLrmiMinPort = serviceDefinition.getInternalPort() + 1;
                        int highLrmiMaxPort = lrmiMaxPort;
                        
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI1"), 
                                "min", String.valueOf(lowLrmiMinPort));
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI1"), 
                                "max", String.valueOf(lowLrmiMaxPort));
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI2"), 
                                "min", String.valueOf(highLrmiMinPort));
                        getEditor().setNodeAttribute(getFixedPointRangeXPath(serviceDefinition.getServiceName(), "LRMI2"), 
                                "max", String.valueOf(highLrmiMaxPort));
                    }

                    String elementName = "InputEndpoint";
                    
                    XMLElementAttribute[] attributes = new XMLElementAttribute[4];
                    attributes[0] = new XMLElementAttribute("name", "Web");
                    attributes[1] = new XMLElementAttribute("protocol", "tcp");
                    attributes[2] = new XMLElementAttribute("port", String.valueOf(serviceDefinition.getExternalPort()));
                    attributes[3] = new XMLElementAttribute("localPort", String.valueOf(serviceDefinition.getInternalPort()));
                    getEditor().addElement(getEndpointsXPath(serviceDefinition.getServiceName()), elementName, attributes);
                    
                } catch (NumberFormatException e) {
                    throw new XMLXPathEditorException(e);
                }
            }
        }
    }
    
    private static String getEndpointsXPath(String roleName) {
        return "/ServiceDefinition/WorkerRole[@name='" + roleName + "']/Endpoints";
    }
    
    private static String getInternalEndpointXPath(String roleName, String internalEndpointName) {
        return getEndpointsXPath(roleName) + "/InternalEndpoint[@name='" + internalEndpointName + "']";
    }
    
    private static String getFixedPointRangeXPath(String roleName, String internalEndpointName) {
        return getInternalEndpointXPath(roleName, internalEndpointName) + "/FixedPortRange";
    }
    
    private static String getMinFixedPointRangeXPath(String roleName, String internalEndpointName) {
        return getFixedPointRangeXPath(roleName, internalEndpointName) + "/@min";
    }
    
    private static String getMaxFixedPointRangeXPath(String roleName, String internalEndpointName) {
        return getFixedPointRangeXPath(roleName, internalEndpointName) + "/@max";
    }
}
