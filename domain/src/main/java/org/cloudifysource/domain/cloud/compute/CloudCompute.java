package org.cloudifysource.domain.cloud.compute;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

import java.util.Map;

/**
 * Holds compute templates.
 * 
 * @see {@link ComputeTemplate}
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "cloudCompute", clazz = CloudCompute.class, allowInternalNode = true, allowRootNode = true,
	parent = "cloud")
public class CloudCompute {
	
	private Map<String, ComputeTemplate> templates;

	public Map<String, ComputeTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, ComputeTemplate> templates) {
		this.templates = templates;
	}

}
