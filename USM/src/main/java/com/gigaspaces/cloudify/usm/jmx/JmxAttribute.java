package com.gigaspaces.cloudify.usm.jmx;


/**
 * Simple object for JMX Attribute
 * 
 * @author barakme
 * @since 8.0.3
 *
 */
public class JmxAttribute implements Comparable<JmxAttribute> {
	
	private String objectName;
	private String attributeName;
	private String displayName;
	private Object value; 
	
	public JmxAttribute(String objectName, String attributeName, String displayName) {
		super();
		this.objectName = objectName;
		this.attributeName = attributeName;
		this.displayName = displayName;
	}
	
	
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}


	@Override
	public String toString() {
		return "JmxAttribute [objectName=" + objectName + ", attributeName=" + attributeName + ", displayName="
				+ displayName + "]";
	}


	public int compareTo(JmxAttribute o) {
		int beanComparison = this.getObjectName().compareTo(o.getObjectName());
		if(beanComparison != 0) {
			return beanComparison;
		}
		return this.getAttributeName().compareTo(o.getAttributeName());
	}
	
	
	

}
