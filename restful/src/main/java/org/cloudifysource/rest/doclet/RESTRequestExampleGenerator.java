/*******************************************************************************

 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.doclet;

import java.util.logging.Logger;

import org.apache.commons.lang.ClassUtils;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.request.SetApplicationAttributesRequest;
import org.cloudifysource.dsl.rest.request.SetServiceAttributesRequest;
import org.cloudifysource.dsl.rest.request.SetServiceInstanceAttributesRequest;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.request.UpdateApplicationAttributeRequest;
import org.cloudifysource.restDoclet.exampleGenerators.IDocExampleGenerator;
import org.cloudifysource.restDoclet.exampleGenerators.PrimitiveExampleValues;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import com.sun.javadoc.Type;

/**
 * 
 * @author yael
 * @since 2.6.0
 *
 */
public class RESTRequestExampleGenerator implements IDocExampleGenerator {
	private static final Logger logger = Logger.getLogger(RESTRequestExampleGenerator.class.getName());
	
	@Override
	public String generateExample(final Type type) 
			throws Exception {
		Class<?> clazz = ClassUtils.getClass(type.qualifiedTypeName());
		Object requestBodyParam = getRequestExample(clazz);
		String jsonStr = new ObjectMapper().writeValueAsString(requestBodyParam);
		return jsonStr;
		
	}
	
	private Object getRequestExample(final Class<?> clazz) {
		Object example = null;
		if (clazz.equals(InstallApplicationRequest.class)) {
			InstallApplicationRequest installApplicationRequest = new InstallApplicationRequest();
			installApplicationRequest.setApplcationFileUploadKey(RESTExamples.getUploadKey());
			installApplicationRequest.setApplicationName(RESTExamples.getAppName());
			installApplicationRequest.setApplicationOverridesUploadKey(RESTExamples.getUploadKey());
			installApplicationRequest.setAuthGroups(RESTExamples.getAuthGroup());
			installApplicationRequest.setCloudConfigurationUploadKey(RESTExamples.getUploadKey());
			installApplicationRequest.setCloudOverridesUploadKey(RESTExamples.getUploadKey());
			installApplicationRequest.setDebugAll(RESTExamples.isDebugAll());
			installApplicationRequest.setDebugEvents(RESTExamples.getDebugEvents());
			installApplicationRequest.setDebugMode(RESTExamples.getDebugMode());
			installApplicationRequest.setSelfHealing(RESTExamples.isSelfHealing());
			example = installApplicationRequest;
		} else if (clazz.equals(InstallServiceRequest.class)) {
			InstallServiceRequest installServiceRequest = new InstallServiceRequest();
			installServiceRequest.setAuthGroups(RESTExamples.getAuthGroup());
			installServiceRequest.setCloudConfigurationUploadKey(RESTExamples.getUploadKey());
			installServiceRequest.setCloudOverridesUploadKey(RESTExamples.getUploadKey());
			installServiceRequest.setDebugAll(RESTExamples.isDebugAll());
			installServiceRequest.setDebugEvents(RESTExamples.getDebugEvents());
			installServiceRequest.setDebugMode(RESTExamples.getDebugMode());
			installServiceRequest.setSelfHealing(RESTExamples.isSelfHealing());
			installServiceRequest.setServiceFileName(RESTExamples.getServiceFileName());
			installServiceRequest.setServiceFolderUploadKey(RESTExamples.getUploadKey());
			installServiceRequest.setServiceOverridesUploadKey(RESTExamples.getUploadKey());
			example = installServiceRequest;
		} else if (clazz.equals(SetApplicationAttributesRequest.class)) {
			example = new SetApplicationAttributesRequest();
			((SetApplicationAttributesRequest) example).setAttributes(RESTExamples.getAttributes());
		} else if (clazz.equals(SetServiceAttributesRequest.class)) {
			example = new SetServiceAttributesRequest();
			((SetServiceAttributesRequest) example).setAttributes(RESTExamples.getAttributes());
		} else if (clazz.equals(SetServiceInstanceAttributesRequest.class)) {
			example = new SetServiceInstanceAttributesRequest();
			((SetServiceInstanceAttributesRequest) example).setAttributes(RESTExamples.getAttributes());
		} else if (clazz.equals(UpdateApplicationAttributeRequest.class)) {
			example = new UpdateApplicationAttributeRequest();
			((UpdateApplicationAttributeRequest) example).setValue(RESTExamples.getAttribute());
		} else if (SetServiceInstancesRequest.class.equals(clazz)) {
			SetServiceInstancesRequest setServiceInstancesRequest = new  SetServiceInstancesRequest();
			setServiceInstancesRequest.setCount(2);
			setServiceInstancesRequest.setLocationAware(false);
			setServiceInstancesRequest.setTimeout(RESTExamples.getTimeoutMinutes());
			example = setServiceInstancesRequest;
		} else if (AddTemplatesRequest.class.equals(clazz)) {
			example = new AddTemplatesRequest();
			((AddTemplatesRequest) example).setUploadKey(RESTExamples.getUploadKey());
		} else if (MultipartFile.class.isAssignableFrom(clazz)) {
			example = "file's content";
		} else if (clazz.isPrimitive()) {
			example = PrimitiveExampleValues.getValue(clazz);
		} else if (clazz.equals(String.class)) {
			example = "string";
		} else {
			String className = clazz.getName();
			logger.warning("Missing custom instantiation of class " + className 
					+ " for generating request example, using newInstance instead.");
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				String errMsg = "failed to instantiate " + className;
				logger.warning(errMsg);
				throw new IllegalArgumentException(errMsg);
			}
		}
		return example;
	}
}
