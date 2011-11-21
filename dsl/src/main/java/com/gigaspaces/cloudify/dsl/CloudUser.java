package com.gigaspaces.cloudify.dsl;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "user", clazz = CloudUser.class, allowInternalNode = true, allowRootNode = false, parent = "cloud2")
public class CloudUser {

	private String user; 
    private String apiKey;
    private String keyFile;
    private String keyPair;
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getKeyFile() {
		return keyFile;
	}
	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}
	public String getKeyPair() {
		return keyPair;
	}
	public void setKeyPair(String keyPair) {
		this.keyPair = keyPair;
	}

    
    
}


