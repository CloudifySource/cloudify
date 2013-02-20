/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.context.kvstorage.spaceentries;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;

/**
 * Base pojo for context properties which is stored in the space.
 * 
 * @author eitany
 * @since 2.0
 */
@SpaceClass
public abstract class AbstractCloudifyAttribute {

	protected AbstractCloudifyAttribute() {
	}

	protected AbstractCloudifyAttribute(final String applicationName, final String key, final Object value) {
		this.applicationName = applicationName;
		this.key = key;
		this.value = value;
	}

	private String applicationName;
	private String key;
	private Object value;
	private String uid;

	@SpaceId(autoGenerate = true)
	public String getUid() {
		return uid;
	}

	public void setUid(final String uid) {
		this.uid = uid;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	@SpaceIndex
	public String getKey() {
		return key;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

}