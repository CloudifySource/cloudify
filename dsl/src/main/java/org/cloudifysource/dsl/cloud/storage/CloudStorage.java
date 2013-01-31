package org.cloudifysource.dsl.cloud.storage;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * 
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "cloudStorage", clazz = CloudStorage.class, allowInternalNode = true, allowRootNode = true,
	parent = "cloud")
public class CloudStorage {

	private Map<String, StorageTemplate> templates = new HashMap<String, StorageTemplate>();

	public Map<String, StorageTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, StorageTemplate> templates) {
		this.templates = templates;
	}
}
