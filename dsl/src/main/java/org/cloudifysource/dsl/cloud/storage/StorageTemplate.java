package org.cloudifysource.dsl.cloud.storage;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * 
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "storageTemplate", clazz = StorageTemplate.class, 
	allowInternalNode = true, allowRootNode = true, parent = "cloudStorage")
public class StorageTemplate {
	
	private int size;

	public int getSize() {
		return size;
	}

	public void setSize(final int size) {
		this.size = size;
	}
}
