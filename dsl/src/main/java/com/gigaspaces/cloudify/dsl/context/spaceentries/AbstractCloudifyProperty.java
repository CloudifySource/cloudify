package com.gigaspaces.cloudify.dsl.context.spaceentries;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;

/**
 * Base pojo for context properties which is stored in the space
 * @author eitany
 * @since 2.0
 */
@SpaceClass
public abstract class AbstractCloudifyProperty {

	private String applicationName;
	private String key;
	private Object value;
	private String uid;

	@SpaceId(autoGenerate = true)
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@SpaceIndex
	public String getKey() {
		return key;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

}