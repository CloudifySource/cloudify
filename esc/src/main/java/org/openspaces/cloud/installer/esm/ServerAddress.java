package org.openspaces.cloud.installer.esm;


public class ServerAddress {

    private String privateAddress;
    private String publicAddress;
    
    public void setPrivateAddress(String privateAddress) {
        this.privateAddress = privateAddress;
    }
    public String getPrivateAddress() {
        return privateAddress;
    }
    public void setPublicAddress(String publicAddress) {
        this.publicAddress = publicAddress;
    }
    public String getPublicAddress() {
        return publicAddress;
    }
    
}
