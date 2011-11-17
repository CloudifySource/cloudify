package com.gigaspaces.cloudify.dsl;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name="memcached", clazz=Memcached.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class Memcached extends ServiceProcessingUnit{
	private int port;
	private int portRetries;
	private boolean threaded;
	private String binaries;
	
	
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}
	public boolean isThreaded() {
		return threaded;
	}
	public void setPortRetries(int portRetries) {
		this.portRetries = portRetries;
	}
	public int getPortRetries() {
		return portRetries;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getPort() {
		return port;
	}

	public void setBinaries(String binaries) {
		this.binaries = binaries;
	}
	
	public String getBinaries() {
		return binaries;
	}
}
