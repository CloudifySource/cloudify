package org.cloudifysource.dsl.cloud.compute;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

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
	
	private Map<String, ComputeTemplate> templates = new HashMap<String, ComputeTemplate>();

	public Map<String, ComputeTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, ComputeTemplate> templates) {
		this.templates = templates;
	}

}
