package com.gigaspaces.cloudify.usm.jmx;


/**
 * A user-defined JMX target make of type, attribute and display name
 * 
 * @author giladh
 * @since 8.0.1
 *
 */
public class JmxTarget {
	
	private String domain;
	private String type;
	private String attr;
	private String dispName;	
	
	private Object value; // place-holder for the attribute's value
	
	
	
	public JmxTarget(String domain, String type, String attr) {		
		this(domain, type, attr, attr/*=default display name*/);
	}

	public JmxTarget(){
		
	}
	public JmxTarget(String domain, String type, String attr, String dispName) {
		this.domain = domain;
		this.type = type;
		this.attr = attr;
		this.dispName = dispName;
		this.value = null;
	}
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAttr() {
		return attr;
	}
	public void setAttr(String attr) {
		this.attr = attr;
	}

	public String getDispName() {
		return dispName;
	}

	public void setDispName(String dispName) {
		this.dispName = dispName;
	}
	
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		if (dispName == null) {
			return domain + ":" + type + ":" + attr;
		}
		return domain + ":" + type + ":" + attr + ":" + dispName;
	}
}
