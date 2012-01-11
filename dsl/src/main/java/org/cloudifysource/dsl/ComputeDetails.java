package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "compute", clazz = ComputeDetails.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class ComputeDetails {

	private String template;

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
}
