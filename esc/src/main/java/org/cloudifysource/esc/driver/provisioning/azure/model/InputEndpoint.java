package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "InputEndpoint", propOrder = { "loadBalancedEndpointSetName",
		"localPort", "name", "port", "protocol" , "vIp" })
public class InputEndpoint {

	@Override
	public String toString() {
		return "InputEndpoint [loadBalancedEndpointSetName="
				+ loadBalancedEndpointSetName + ", localPort=" + localPort
				+ ", name=" + name + ", port=" + port + ", protocol="
				+ protocol + ", vIp=" + vIp + "]";
	}

	private String loadBalancedEndpointSetName;
	private int localPort;
	private String name;
	private int port;
	private String protocol;
	private String vIp;
	
	/**
	 * 
	 * @return - Public IP for this endpoint.
	 */
	@XmlElement(name = "Vip")
	public String getvIp() {
		return vIp;
	}

	/**
	 * 
	 * @param vIp .
	 */
	public void setvIp(final String vIp) {
		this.vIp = vIp;
	}

	@XmlElement(name = "LoadBalancedEndpointSetName")
	public String getLoadBalancedEndpointSetName() {
		return loadBalancedEndpointSetName;
	}

	public void setLoadBalancedEndpointSetName(
			final String loadBalancedEndpointSetName) {
		this.loadBalancedEndpointSetName = loadBalancedEndpointSetName;
	}

	@XmlElement(name = "LocalPort")
	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(final int localPort) {
		this.localPort = localPort;
	}

	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@XmlElement(name = "Port")
	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	@XmlElement(name = "Protocol")
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}
}
