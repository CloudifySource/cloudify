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
	private String namePrefix;
	private String location;

	public String getLocation() {
		return location;
	}

	public void setLocation(final String location) {
		this.location = location;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(final String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public int getSize() {
		return size;
	}

	public void setSize(final int size) {
		this.size = size;
	}
}
