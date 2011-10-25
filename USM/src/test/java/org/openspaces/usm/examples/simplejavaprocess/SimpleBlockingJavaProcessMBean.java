package org.openspaces.usm.examples.simplejavaprocess;

public interface SimpleBlockingJavaProcessMBean {
	
	public String getDetails();
	public String getType();
	public int getCounter();
	public void die();
	
}
