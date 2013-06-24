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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ClassUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.DeleteApplicationAttributeResponse;
import org.cloudifysource.dsl.rest.response.DeleteServiceAttributeResponse;
import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.GetApplicationAttributesResponse;
import org.cloudifysource.dsl.rest.response.GetServiceAttributesResponse;
import org.cloudifysource.dsl.rest.response.GetServiceInstanceAttributesResponse;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.cloudifysource.dsl.rest.response.ServiceInstanceDetails;
import org.cloudifysource.dsl.rest.response.ServiceInstanceMetricsResponse;
import org.cloudifysource.dsl.rest.response.ServiceMetricsResponse;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UpdateApplicationAttributeResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.restDoclet.exampleGenerators.IDocResponseExampleGenerator;
import org.cloudifysource.restDoclet.exampleGenerators.PrimitiveExampleValues;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.Type;

/**
 * 
 * @author yael
 * @since 2.6.0
 *
 */
public class RESTResposneExampleGenerator implements IDocResponseExampleGenerator {

	private static final Logger logger = Logger.getLogger(RESTResposneExampleGenerator.class.getName());

	@Override
	public String generateResponseExample(final Type type) throws Exception {	

		Response<Object> responseWrapper = new Response<Object>();
		responseWrapper.setResponse(getExample(type));
		responseWrapper.setStatus("Success");
		responseWrapper.setMessage("Operation completed successfully");
		responseWrapper.setMessageId(CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName());
		
		return new ObjectMapper().writeValueAsString(responseWrapper);
	}

	private Object getExample(final Type type) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Object example;
		Class<?> clazz = ClassUtils.getClass(type.qualifiedTypeName());

		// create the example instance 
		if (DeleteApplicationAttributeResponse.class.equals(clazz)) {
			example = new DeleteApplicationAttributeResponse();
			((DeleteApplicationAttributeResponse) example).setPreviousValue(RESTExamples.getAttribute());
		} else if (DeleteServiceAttributeResponse.class.equals(clazz)) {
			example = new DeleteServiceAttributeResponse();
			((DeleteServiceAttributeResponse) example).setPreviousValue(RESTExamples.getAttribute());
		} else if (DeleteServiceInstanceAttributeResponse.class.equals(clazz)) {
			example = new DeleteServiceInstanceAttributeResponse();
			((DeleteServiceInstanceAttributeResponse) example).setPreviousValue(RESTExamples.getAttribute());
		} else if (GetApplicationAttributesResponse.class.equals(clazz)) {
			example = new GetApplicationAttributesResponse();
			((GetApplicationAttributesResponse) example).setAttributes(RESTExamples.getAttributes());
		} else if (GetServiceAttributesResponse.class.equals(clazz)) {
			example = new GetServiceAttributesResponse();
			((GetServiceAttributesResponse) example).setAttributes(RESTExamples.getAttributes());
		} else if (GetServiceInstanceAttributesResponse.class.equals(clazz)) {
			example = new GetServiceInstanceAttributesResponse();
			((GetServiceInstanceAttributesResponse) example).setAttributes(RESTExamples.getAttributes());
		} else if (InstallApplicationResponse.class.equals(clazz)) {
			example = new InstallApplicationResponse();
			((InstallApplicationResponse) example).setDeploymentID(RESTExamples.getDeploymentID());
		} else if (InstallServiceResponse.class.equals(clazz)) {
			example = new InstallServiceResponse();
			((InstallServiceResponse) example).setDeploymentID(RESTExamples.getDeploymentID());
		} else if (ServiceInstanceMetricsResponse.class.equals(clazz)) {
			example = new ServiceInstanceMetricsResponse();
			((ServiceInstanceMetricsResponse) example).setAppName(RESTExamples.getAppName());
			((ServiceInstanceMetricsResponse) example).setServiceInstanceMetricsData(
					RESTExamples.getServiceInstanceMetricsData());
			((ServiceInstanceMetricsResponse) example).setServiceName(RESTExamples.getServiceName());
		} else if (ServiceMetricsResponse.class.equals(clazz)) {
			ServiceMetricsResponse serviceMetricsResponse = new ServiceMetricsResponse();
			serviceMetricsResponse.setAppName(RESTExamples.getAppName());
			serviceMetricsResponse.setServiceInstaceMetricsData(RESTExamples.getServiceInstanceMetricsDataList());
			serviceMetricsResponse.setServiceName(RESTExamples.getServiceName());
			example = serviceMetricsResponse;
		} else if (UninstallApplicationResponse.class.equals(clazz)) {
			example = new UninstallApplicationResponse();
			((UninstallApplicationResponse) example).setDeploymentID(RESTExamples.getDeploymentID());
		} else if (UninstallServiceResponse.class.equals(clazz)) {
			example = new UninstallServiceResponse();
			((UninstallServiceResponse) example).setDeploymentID(RESTExamples.getDeploymentID());
		} else if (UpdateApplicationAttributeResponse.class.equals(clazz)) {
			example = new UpdateApplicationAttributeResponse();
			((UpdateApplicationAttributeResponse) example).setPreviousValue(RESTExamples.getAttribute());
		} else if (UploadResponse.class.equals(clazz)) {
			example = new UploadResponse();
			((UploadResponse) example).setUploadKey(RESTExamples.getUploadKey());	
		} else if (ServiceDetails.class.equals(clazz)) {
			ServiceDetails serviceDetails = new ServiceDetails();
			serviceDetails.setApplicationName(RESTExamples.getAppName());
			serviceDetails.setInstanceNames(RESTExamples.getInstanceNames());
			serviceDetails.setName(RESTExamples.getServiceName());
			serviceDetails.setNumberOfInstances(RESTExamples.getNumberOfInstances());
			example = serviceDetails;
		} else if (DeploymentEvents.class.equals(clazz)) {
			example = new DeploymentEvents();
			((DeploymentEvents) example).setEvents(RESTExamples.getEvents());
		} else if (ApplicationDescription.class.equals(clazz)) {
			ApplicationDescription applicationDescription = new ApplicationDescription();
			applicationDescription.setApplicationName(RESTExamples.getAppName());
			applicationDescription.setApplicationState(RESTExamples.getApplicationState());
			applicationDescription.setAuthGroups(RESTExamples.getAuthGroups());
			applicationDescription.setServicesDescription(RESTExamples.getServicesDescription());
			example = applicationDescription;
		} else if (ServiceInstanceDetails.class.equals(clazz)) {
			ServiceInstanceDetails serviceInstanceDetails = new ServiceInstanceDetails();
			
			String appName = RESTExamples.getAppName();
			String serviceName = RESTExamples.getServiceName();
			int instanceId = RESTExamples.getInstanceId();
			
			serviceInstanceDetails.setApplicationName(appName);
			serviceInstanceDetails.setHardwareId(RESTExamples.getHardwareId());
			serviceInstanceDetails.setImageId(RESTExamples.getImageId());
			serviceInstanceDetails.setInstanceId(instanceId);
			serviceInstanceDetails.setMachineId(RESTExamples.getMachineId());
			serviceInstanceDetails.setPrivateIp(RESTExamples.getPrivateIp());
			serviceInstanceDetails.setProcessDetails(RESTExamples.getProcessDetails(instanceId));
			serviceInstanceDetails.setPublicIp(RESTExamples.getPublicIp());
			serviceInstanceDetails.setServiceInstanceName(RESTExamples.getInstanceName(serviceName, appName));
			serviceInstanceDetails.setServiceName(serviceName);
			serviceInstanceDetails.setTemplateName(RESTExamples.getTemplateName());
			
			example = serviceInstanceDetails;
		} else if (ServiceDescription.class.equals(clazz)) {
			example = RESTExamples.getServicesDescription();
		}  else if (clazz.isPrimitive()) {
			example = PrimitiveExampleValues.getValue(clazz);
		} else if (String.class.equals(clazz)) {
			example = "string";
		} else if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
			List<Object> list = new LinkedList<Object>();
			if (List.class.isAssignableFrom(clazz)) {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Type paramType = parameterizedType.typeArguments()[0];
				list.add(getExample(paramType));
				list.add(getExample(paramType));
				list.add(getExample(paramType));
			} 
			example = list;
		} else {
			String className = clazz.getName();
			logger.warning("Missing custom instantiation of class " + className 
					+ " for generating response example, using newInstance instead.");
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
