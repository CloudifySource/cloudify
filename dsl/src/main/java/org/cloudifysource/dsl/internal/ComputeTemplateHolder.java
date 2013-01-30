package org.cloudifysource.dsl.internal;

import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * 
 * @author yael
 *
 */
@SpaceClass
public class ComputeTemplateHolder {
	private String name;
	private ComputeTemplate cloudTemplate;
	private String templateFileName;
	private String propertiesFileName;
	private String overridesFileName;
	
	public ComputeTemplateHolder() {
		
	}
	
	@SpaceId
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public ComputeTemplate getCloudTemplate() {
		return cloudTemplate;
	}
	public void setCloudTemplate(final ComputeTemplate cloudTemplate) {
		this.cloudTemplate = cloudTemplate;
	}

	public String getTemplateFileName() {
		return templateFileName;
	}

	public void setTemplateFileName(final String templateFileName) {
		this.templateFileName = templateFileName;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(final String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}

	public String getOverridesFileName() {
		return overridesFileName;
	}

	public void setOverridesFileName(final String overridesFileName) {
		this.overridesFileName = overridesFileName;
	}
	
}
