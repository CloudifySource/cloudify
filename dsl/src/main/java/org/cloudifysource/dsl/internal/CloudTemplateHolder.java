package org.cloudifysource.dsl.internal;

import java.io.Serializable;

import org.cloudifysource.dsl.cloud.CloudTemplate;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * 
 * @author yael
 *
 */
@SpaceClass
public class CloudTemplateHolder implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private CloudTemplate cloudTemplate;
	
	public CloudTemplateHolder() {
		
	}
	
	@SpaceId
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public CloudTemplate getCloudTemplate() {
		return cloudTemplate;
	}
	public void setCloudTemplate(final CloudTemplate cloudTemplate) {
		this.cloudTemplate = cloudTemplate;
	}
	
}
