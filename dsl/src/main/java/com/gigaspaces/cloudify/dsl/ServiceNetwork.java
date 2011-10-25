package com.gigaspaces.cloudify.dsl;

import java.io.Serializable;

/**
 * Configuration of network elements of a specific service
 * @author itaif
 *
 */
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
