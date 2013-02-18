package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType
public class InputEndpoints {

	@Override
	public String toString() {
		return "InputEndpoints [inputEndpoints=" + inputEndpoints + "]";
	}

	private List<InputEndpoint> inputEndpoints = new ArrayList<InputEndpoint>();

	@XmlElement(name = "InputEndpoint")
	public List<InputEndpoint> getInputEndpoints() {
		return inputEndpoints;
	}

	public void setInputEndpoints(final List<InputEndpoint> inputEndpoints) {
		this.inputEndpoints = inputEndpoints;
	}
}
