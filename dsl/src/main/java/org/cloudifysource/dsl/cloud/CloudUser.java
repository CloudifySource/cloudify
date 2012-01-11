package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name = "user", clazz = CloudUser.class, allowInternalNode = true, allowRootNode = false, parent = "cloud")
public class CloudUser {

	private String user; 
    private String apiKey;
    private String keyFile;
    
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

	@Override
	public String toString() {
		return "CloudUser [user=" + user + ", keyFile=" + keyFile
				+ "]";
	}

    
    
}


