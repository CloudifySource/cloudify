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
package org.cloudifysource.rest.util;

import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_APP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.InstanceDescription;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitInstanceStatistics;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.pu.service.ServiceMonitors;
/**
 * This factory class is responsible for manufacturing an application description POJO.
 * The application description will consist of all the application's services and their status.
 * The application status is made out of an intersection between all of it's service's status.
 * A service status is determined by the status of all it's service instances.   
 * 
 * @author adaml
 *
 */
public class ApplicationDescriptionFactory {
	private Admin admin;
	
	private static final Logger logger = Logger
			.getLogger(ApplicationDescriptionFactory.class.getName());
	
	public ApplicationDescriptionFactory(final Admin admin) {
		this.admin = admin;
	}
	
	/**
	 * returns an application description POJO.
	 * 
	 * @param applicationName the application name.
	 * @return 
	 * 		the application description.
	 * @throws RestErrorException 
	 * 		if application is not found.
	 */
	public ApplicationDescription getApplicationDescription(final String applicationName) 
			throws RestErrorException {
		final Application app = admin.getApplications().waitFor(applicationName, 5, TimeUnit.SECONDS);
		if (app == null) {
			throw new RestErrorException(FAILED_TO_LOCATE_APP, applicationName);
		}
		
		List<ServiceDescription> serviceDescriptionList = getServicesDescription(
				applicationName, app);
		logger.log(Level.FINE, "Creating application description for application " + app.getName());
		DeploymentState applicationState = getApplicationState(serviceDescriptionList);
		
		ApplicationDescription applicationDescription = new ApplicationDescription();
		applicationDescription.setApplicationName(applicationName);
		applicationDescription.setServicesDescription(serviceDescriptionList);
		applicationDescription.setApplicationState(applicationState);
		
		return applicationDescription;
	}

	private List<ServiceDescription> getServicesDescription(
			final String applicationName, final Application app) {
		List<ServiceDescription> serviceDescriptionList = new ArrayList<ServiceDescription>();
		final ProcessingUnits pus = app.getProcessingUnits();
		for (final ProcessingUnit pu : pus) {
			String absolutePuName = pu.getName();
			ServiceDescription serviceDescription = getServiceDescription(absolutePuName, applicationName); 
			serviceDescriptionList.add(serviceDescription);
		}
		return serviceDescriptionList;
	}
	

	private ServiceDescription getServiceDescription(final String absolutePuName, final String applicationName) {

		String serviceName = ServiceUtils.getApplicationServiceName(absolutePuName, applicationName);
		int plannedNumberOfInstances = getPlannedNumberOfInstances(absolutePuName);
		int numberOfServiceInstances = getNumberOfServiceInstances(absolutePuName);
		List<InstanceDescription> serviceInstancesDescription = getServiceInstacesDescription(absolutePuName);
		DeploymentState serviceState = getServiceState(serviceInstancesDescription, absolutePuName);
		
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setPlannedInstances(plannedNumberOfInstances);
		serviceDescription.setInstanceCount(numberOfServiceInstances);
		serviceDescription.setApplicationName(applicationName);
		serviceDescription.setServiceName(serviceName);
		serviceDescription.setInstancesDescription(serviceInstancesDescription);
		serviceDescription.setServiceState(serviceState);
		
		return serviceDescription;
	}
	
	private InstanceDescription getInstanceDescription(final ProcessingUnitInstance processingUnitInstance) {
		String instanceState = getInstanceState(processingUnitInstance);
		int instanceId = processingUnitInstance.getInstanceId();
		String instanceHostName = processingUnitInstance.getVirtualMachine().getMachine().getHostName();
		String instanceName = processingUnitInstance.getName();
		
		InstanceDescription instanceDescription = new InstanceDescription();
		instanceDescription.setInstanceStatus(instanceState);
		instanceDescription.setInstanceName(instanceName);
		instanceDescription.setInstanceId(instanceId);
		instanceDescription.setHostName(instanceHostName);
		
		return instanceDescription;
	}

	private String getInstanceState(
			final ProcessingUnitInstance processingUnitInstance) {
		String instanceState;
		ProcessingUnit processingUnit = processingUnitInstance.getProcessingUnit();
		if (processingUnit.getType().equals(ProcessingUnitType.UNIVERSAL)) {
			instanceState = getInstanceUsmState(processingUnitInstance).toString();
		} else {
			instanceState = processingUnit.getStatus().toString();
		}
		return instanceState;
	}
	
	private List<InstanceDescription> getServiceInstacesDescription(
			final String absolutePuName) {
		ProcessingUnit processingUnit = getProcessingUnit(absolutePuName);
		List<InstanceDescription> instancesDescriptionList = new ArrayList<InstanceDescription>();
		
		for (ProcessingUnitInstance processingUnitInstance : processingUnit) {
			InstanceDescription instanceDescription = getInstanceDescription(processingUnitInstance);
            instancesDescriptionList.add(instanceDescription);
		}
		
		return instancesDescriptionList;
	}
	
	//This method will return failed status only if all of the services reached a final state.
	private DeploymentState getApplicationState(final 
			List<ServiceDescription> serviceDescriptionList) {
		logger.log(Level.FINE, "Determining services deployment state");
		boolean servicesStillInstalling = false;
		boolean atLeastOneServiceFailed = false;
		for (ServiceDescription serviceDescription : serviceDescriptionList) {
			logger.log(Level.FINE, "checking status for service " + serviceDescription.getServiceName());
			if (serviceDescription.getServiceState().equals(DeploymentState.INSTALLING)
					|| serviceDescriptionList.isEmpty()) {
				servicesStillInstalling = true;
			}
			if (serviceDescription.getServiceState().equals(DeploymentState.FAILED)) {
				atLeastOneServiceFailed = true;
			}
			
		}
		if (servicesStillInstalling) {
			return DeploymentState.INSTALLING;
		} 
		if (atLeastOneServiceFailed) {
			return DeploymentState.FAILED;
		}
		return DeploymentState.STARTED;
	}
	
	private DeploymentState getServiceState(final List<InstanceDescription> serviceInstancesStatus, 
			final String absolutePuName) {
		ProcessingUnit pu = getProcessingUnit(absolutePuName);
		logger.log(Level.FINE, "Determining service state for service " + absolutePuName);
		if (pu.getType().equals(ProcessingUnitType.UNIVERSAL)) {
			for (InstanceDescription instanceDescription : serviceInstancesStatus) {
				String instanceState = instanceDescription.getInstanceStatus();
				if (instanceState.equals(USMState.ERROR.toString())) {
					return DeploymentState.FAILED;
				} 
			}
			if (getNumberOfServiceInstances(absolutePuName) != getPlannedNumberOfInstances(absolutePuName)) {
				return DeploymentState.INSTALLING;
			}
			return DeploymentState.STARTED;

		} else { //The service is not a USM service.
			if (!pu.getStatus().equals(DeploymentStatus.INTACT)) {
				return DeploymentState.INSTALLING;
			} else {
				return DeploymentState.STARTED;
			}
		}
	}

	private int getNumberOfServiceInstances(final String absolutePuName) {
        ProcessingUnit processingUnit = getProcessingUnit(absolutePuName);

        if (processingUnit != null) {
            if (processingUnit.getType().equals(
                    ProcessingUnitType.UNIVERSAL)) {
                return getNumberOfUSMServicesWithRunningState(absolutePuName);
            }

            return getProcessingUnit(absolutePuName).getInstances().length;
        }
        return 0;
    }

	private ProcessingUnit getProcessingUnit(final String absolutePuName) {
		return admin.getProcessingUnits().getProcessingUnit(absolutePuName);
	}
    
    // returns the number of RUNNING processing unit instances.
    private int getNumberOfUSMServicesWithRunningState(
            final String absolutePUName) {

        int puInstanceCounter = 0;
        ProcessingUnit processingUnit = getProcessingUnit(absolutePUName);

        for (ProcessingUnitInstance pui : processingUnit) {
            if (isUsmStateOfPuiRunning(pui)) {
                puInstanceCounter++;
            }
        }
        return puInstanceCounter;
    }

    private boolean isUsmStateOfPuiRunning(final ProcessingUnitInstance pui) {
        USMState instanceState = getInstanceUsmState(pui);
        if (instanceState.equals(CloudifyConstants.USMState.RUNNING)) {
            return true;
        }
        return false;
    }

    //returns the USM state of a 
	private USMState getInstanceUsmState(final ProcessingUnitInstance pui) {
		ProcessingUnitInstanceStatistics statistics = pui.getStatistics();
        if (statistics == null) {
            return null;
        }
        Map<String, ServiceMonitors> puMonitors = statistics.getMonitors();
        if (puMonitors == null) {
            return null;
        }
        ServiceMonitors serviceMonitors = puMonitors.get("USM");
        if (serviceMonitors == null) {
            return null;
        }
        Map<String, Object> monitors = serviceMonitors.getMonitors();
        if (monitors == null) {
            return null;
        }
        
        USMState usmState = USMState.values()[(Integer) monitors.get(CloudifyConstants.USM_MONITORS_STATE_ID)];
		return usmState;
	}
	
	//returns a service's planned number of instances.
	private int getPlannedNumberOfInstances(final String absolutePuName) {
		ProcessingUnit processingUnit = getProcessingUnit(absolutePuName);
		Map<String, String> elasticProperties = ((DefaultProcessingUnit) processingUnit).getElasticProperties();
		int plannedNumberOfInstances;
		if (elasticProperties.containsKey("schema")) {
			String clusterSchemaValue = elasticProperties.get("schema");
            if ("partitioned-sync2backup".equals(clusterSchemaValue)) {
            	plannedNumberOfInstances = processingUnit.getTotalNumberOfInstances();
            } else {
            	plannedNumberOfInstances = processingUnit.getNumberOfInstances();
            }
		} else {
			plannedNumberOfInstances = processingUnit.getNumberOfInstances();
		}
		return plannedNumberOfInstances;
	}
}
