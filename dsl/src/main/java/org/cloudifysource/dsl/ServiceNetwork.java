package org.cloudifysource.dsl;

import java.io.Serializable;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


/**
 * Configuration of network elements of a specific service
 * @author itaif
 *
 */
@CloudifyDSLEntity(name="network", clazz=ServiceNetwork.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class ServiceNetwork implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private int port;
    private String protocolDescription;
    
    public ServiceNetwork() {
        
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocolDescription() {
        return protocolDescription;
    }

    public void setProtocolDescription(String protocolDescription) {
        this.protocolDescription = protocolDescription;
    }
}
