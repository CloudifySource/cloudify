package org.cloudifysource.dsl.cloud.compute;

import java.util.LinkedHashMap;
import java.util.Map;

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
	
	private Map<String, ComputeTemplate> templates = new LinkedHashMap<String, ComputeTemplate>(); // TODO - remove this and let the dsl reader initialize it.!!

	public Map<String, ComputeTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, ComputeTemplate> templates) {
		this.templates = templates;
	}

}
