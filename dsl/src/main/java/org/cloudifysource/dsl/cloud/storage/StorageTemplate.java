package org.cloudifysource.dsl.cloud.storage;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String, Object> custom = new HashMap<String, Object>();
	private boolean deleteOnExit = false;
	private String path;
	private String fileSystemType;
	private String deviceName;

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

	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	public void setDeleteOnExit(final boolean deleteOnExit) {
		this.deleteOnExit = deleteOnExit;
	}

	public Map<String, Object> getCustom() {
		return custom;
	}

	public void setCustom(final Map<String, Object> custom) {
		this.custom = custom;
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public String getFileSystemType() {
		return fileSystemType;
	}

	public void setFileSystemType(final String formatType) {
		this.fileSystemType = formatType;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(final String deviceName) {
		this.deviceName = deviceName;
	}
	
}
