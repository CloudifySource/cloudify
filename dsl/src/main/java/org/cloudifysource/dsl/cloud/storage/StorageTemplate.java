/*******************************************************************************
 ' * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.cloud.storage;

import java.io.Serializable;
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
public class StorageTemplate implements Serializable {

	private transient int size;
	private transient String namePrefix;
	private transient Map<String, Object> custom = new HashMap<String, Object>();
	private transient boolean deleteOnExit = false;
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
