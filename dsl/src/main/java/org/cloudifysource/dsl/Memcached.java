package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name="memcached", clazz=Memcached.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class Memcached extends ServiceProcessingUnit{
	private Integer port;
	private Integer portRetries;
	private boolean threaded;
	private String binaries;
	
	
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}
	public boolean isThreaded() {
		return threaded;
	}
	public void setPortRetries(Integer portRetries) {
		this.portRetries = portRetries;
	}
	public Integer getPortRetries() {
		return portRetries;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public Integer getPort() {
		return port;
	}

	public void setBinaries(String binaries) {
		this.binaries = binaries;
	}
	
	public String getBinaries() {
		return binaries;
	}
}
