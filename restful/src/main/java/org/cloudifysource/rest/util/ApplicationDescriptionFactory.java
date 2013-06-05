/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.util;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.InstanceDescription;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.pu.*;
import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.zone.Zones;
import org.openspaces.pu.service.ServiceMonitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory class is responsible for manufacturing an application description POJO. The application description will
 * consist of all the application's services and their status. The application status is made out of an intersection
 * between all of it's service's status. A service status is determined by the status of all it's service instances.
 *
 * @author adaml
 *
 */
public class ApplicationDescriptionFactory {
    private final Admin admin;

    private static final Logger logger = Logger
            .getLogger(ApplicationDescriptionFactory.class.getName());

    public ApplicationDescriptionFactory(final Admin admin) {
        this.admin = admin;
    }

    /**
     * returns an application description POJO.
     *
     * @param applicationName
     *            the application name.
     * @return the application description.
     * @throws ResourceNotFoundException
     *             thrown if one or more of the expected zones were not found.
     */
    public ApplicationDescription getApplicationDescription(final String applicationName) {

        final Application application = admin.getApplications().getApplication(applicationName);

        return getApplicationDescription(application);
    }

    /**
     * returns an application description POJO.
     *
     * @param application
     *            the application name.
     * @return the application description.
     * @throws ResourceNotFoundException
     *             thrown if one or more of the expected zones were not found.
     */
    public ApplicationDescription getApplicationDescription(final Application application) {

        String applicationName = application.getName();
        List<ServiceDescription> serviceDescriptionList = getServicesDescription(
                applicationName, application);
        logger.log(Level.FINE, "Creating application description for application " + applicationName);
        final DeploymentState applicationState = getApplicationState(serviceDescriptionList);

        final ApplicationDescription applicationDescription = new ApplicationDescription();
        applicationDescription.setApplicationName(applicationName);
        applicationDescription.setAuthGroups(getApplicationAuthorizationGroups(application));
        applicationDescription.setServicesDescription(serviceDescriptionList);
        applicationDescription.setApplicationState(applicationState);

        return applicationDescription;
    }

    /**
     * Gets a list of {@link ServiceDescription} objects, representing the application's services.
     *
     * @param applicationName
     *            The name of the application containing the services
     * @param app
     *            The {@link Application} object of the application containing the services
     * @return a list of {@link ServiceDescription} objects, representing the application's services.
     */
    private List<ServiceDescription> getServicesDescription(
            final String applicationName, final Application app) {
        List<ServiceDescription> serviceDescriptionList = new ArrayList<ServiceDescription>();
        final ProcessingUnits pus = app.getProcessingUnits();
        for (final ProcessingUnit pu : pus) {
            final String absolutePuName = pu.getName();
            final ServiceDescription serviceDescription = getServiceDescription(absolutePuName, applicationName);
            serviceDescriptionList.add(serviceDescription);
        }
        return serviceDescriptionList;
    }

    public ServiceDescription getServiceDescription(final ProcessingUnit processingUnit) {
        int plannedNumberOfInstances, numberOfServiceInstances;
        DeploymentState serviceState;
        plannedNumberOfInstances = getPlannedNumberOfInstances(processingUnit);
        numberOfServiceInstances = getNumberOfServiceInstances(processingUnit);
        List<InstanceDescription> serviceInstancesDescription = getServiceInstacesDescription(processingUnit);
        serviceState = getServiceState(processingUnit, serviceInstancesDescription, numberOfServiceInstances,
                plannedNumberOfInstances);
        String absolutePuName = processingUnit.getName();
        logger.log(Level.FINE, "Service \"" + absolutePuName + "\" is in state: " + serviceState);

        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setPlannedInstances(plannedNumberOfInstances);
        serviceDescription.setInstanceCount(numberOfServiceInstances);
        String applicationName = processingUnit.getName().split("\\.")[1];
        serviceDescription.setApplicationName(applicationName);
        serviceDescription.setServiceName(ServiceUtils.getApplicationServiceName(absolutePuName, applicationName));
        serviceDescription.setInstancesDescription(serviceInstancesDescription);
        serviceDescription.setServiceState(serviceState);

        return serviceDescription;


    }

    /**
     * Gets a populated service description object for the specified service.
     *
     * @param absolutePuName
     *            The full service name (<application name>.<service name>)
     * @param applicationName
     *            The name of the application that contains this service
     * @return A populated service description object
     * @throws ResourceNotFoundException
     *             Thrown if a matching zone was not found
     */
    public ServiceDescription getServiceDescription(final String absolutePuName, final String applicationName) {

        Zone zone;
        ProcessingUnit processingUnit = null;

        zone = getZone(absolutePuName);
        if (zone != null) {
            // for undeply - zone exists, PU does not.
            ProcessingUnitInstance[] processingUnitInstances = zone.getProcessingUnitInstances();
            zone.getGridServiceContainers().getContainers()[0].getProcessingUnitInstances();
            if (processingUnitInstances.length > 0) {
                processingUnit = processingUnitInstances[0].getProcessingUnit();
            }
        }

        // if PU not found in zone, perhaps GSCs are not started yet, so look for PU in Admin.
        if (processingUnit == null) {
            // for deploy - zone does not exist, PU does.
            processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePuName);
        }

        if (processingUnit == null) {
            return null;
        }

        return getServiceDescription(processingUnit);
    }

    /**
     * Gets a populated {@link InstanceDescription} object describing the instance (name, id, state, etc.).
     *
     * @param processingUnitInstance
     *            The processing unit instance to describe
     * @return a populated {@link InstanceDescription} object describing the instance (name, id, state, etc.)
     */
    private InstanceDescription getInstanceDescription(final ProcessingUnitInstance processingUnitInstance) {
        final String instanceState = getInstanceState(processingUnitInstance);
        final int instanceId = processingUnitInstance.getInstanceId();
        final String instanceHostName = processingUnitInstance.getVirtualMachine().getMachine().getHostName();
        final String instanceHostAddress = processingUnitInstance.getVirtualMachine().getMachine().getHostAddress();
        final String instanceName = processingUnitInstance.getName();

        final InstanceDescription instanceDescription = new InstanceDescription();
        instanceDescription.setInstanceStatus(instanceState);
        instanceDescription.setInstanceName(instanceName);
        instanceDescription.setInstanceId(instanceId);
        instanceDescription.setHostName(instanceHostName);
        instanceDescription.setHostAddress(instanceHostAddress);

        return instanceDescription;
    }

    /**
     * Get the state of a processing unit instance.
     *
     * @param processingUnitInstance
     *            The processing unit instance to examine
     * @return the state of a processing unit instance.
     */
    private String getInstanceState(
            final ProcessingUnitInstance processingUnitInstance) {
        String instanceState;
        final ProcessingUnit processingUnit = processingUnitInstance.getProcessingUnit();
        if (processingUnit.getType() == ProcessingUnitType.UNIVERSAL) {
            instanceState = getInstanceUsmState(processingUnitInstance).toString();
        } else {
            instanceState = processingUnit.getStatus().toString();
        }
        return instanceState;
    }

    /**
     * Gets a list of {@link InstanceDescription} objects, describing the service instances.
     *
     * @param processingUnit
     *            The service's processing unit of which instances are described
     * @return a list of {@link InstanceDescription} objects, describing the service instances.
     */
    private List<InstanceDescription> getServiceInstacesDescription(
            final ProcessingUnit processingUnit) {

        List<InstanceDescription> instancesDescriptionList = new ArrayList<InstanceDescription>();

        if (processingUnit != null) {
            for (ProcessingUnitInstance processingUnitInstance : processingUnit) {
                InstanceDescription instanceDescription = getInstanceDescription(processingUnitInstance);
                instancesDescriptionList.add(instanceDescription);
            }
        }

        return instancesDescriptionList;
    }

    /**
     * Gets the application state - STARTED, INSTALLING or FAILED. The method returns FAILED status only if all of the
     * services reached a final state.
     *
     * @param serviceDescriptionList
     *            a list of {@link ServiceDescription} objects, representing the application's services' state.
     * @return The applications' state
     */
    private DeploymentState getApplicationState(
            final List<ServiceDescription> serviceDescriptionList) {
        logger.log(Level.FINE, "Determining services deployment state");
        boolean servicesStillInstalling = false;
        boolean atLeastOneServiceFailed = false;
        for (final ServiceDescription serviceDescription : serviceDescriptionList) {
            logger.log(Level.FINE, "checking status for service " + serviceDescription.getServiceName());
            if (serviceDescription.getServiceState() == DeploymentState.IN_PROGRESS
                    || serviceDescriptionList.isEmpty()) {
                servicesStillInstalling = true;
            }
            if (serviceDescription.getServiceState() == DeploymentState.FAILED) {
                atLeastOneServiceFailed = true;
            }

        }
        if (servicesStillInstalling) {
            return DeploymentState.IN_PROGRESS;
        }
        if (atLeastOneServiceFailed) {
            return DeploymentState.FAILED;
        }
        return DeploymentState.STARTED;
    }

    /**
     * Gets a zone by its name.
     *
     * @param zoneName
     *            The name of the requested zone
     * @return The zone matching the specified name, if found. Null otherwise.
     * @throws ResourceNotFoundException
     *             Indicates the zone was not found
     */
    private Zone getZone(final String zoneName) {
        Zone zone = null;
        Zones zones = admin.getZones();
        if (zones != null) {
            zone = zones.getByName(zoneName);
        }

        return zone;
    }

    /**
     * Gets the state of the specified service - STARTED, INSTALLING. If the zone is null - the service state is
     * uninstalled If the zone is not null but the PU is null- the service is currently being uninstalled. If PU
     * instances are found - the service is either running, installing, or in error, depending on the instances' states.
     *
     * @param processingUnit
     *            the service's processing unit (optionally null)
     * @param serviceInstancesStatus
     *            a list of {@link InstanceDescription} objects representing the service's instances' state
     * @return DeploymentState populated with service state details
     */
    private DeploymentState getServiceState(
            final ProcessingUnit processingUnit,
            final List<InstanceDescription> serviceInstancesStatus,
            final int numberOfServiceInstances,
            final int plannedNumberOfInstances) {

        // if the zone is found but doesn't contain an active PU, or the number of running instances is larger than
        // the planned number of resources - the service is currently being uninstalled.
        if (processingUnit == null || numberOfServiceInstances > plannedNumberOfInstances) {
            return DeploymentState.IN_PROGRESS;
        }

        // PU instances found - the service is either running, installing, or in error
        if (processingUnit.getType() == ProcessingUnitType.UNIVERSAL) {
            for (InstanceDescription instanceDescription : serviceInstancesStatus) {
                String instanceState = instanceDescription.getInstanceStatus();
                if (instanceState.equals(USMState.ERROR.toString())) {
                    return DeploymentState.FAILED;
                }
            }
            if (numberOfServiceInstances < plannedNumberOfInstances) {
                return DeploymentState.IN_PROGRESS;
            }
            return DeploymentState.STARTED;

        } else { // The service is not a USM service.
            if (processingUnit.getStatus() != DeploymentStatus.INTACT) {
                return DeploymentState.IN_PROGRESS;
            } else {
                return DeploymentState.STARTED;
            }
        }
    }

    /**
     * Gets a service's number of instances.
     *
     * @param processingUnit
     *            The processing unit implementing this service
     * @return the planned number of instances for the specified service (PU)
     */
    private int getNumberOfServiceInstances(final ProcessingUnit processingUnit) {

        if (processingUnit != null) {
            if (processingUnit.getType() == ProcessingUnitType.UNIVERSAL) {
                return getNumberOfUSMServicesWithRunningState(processingUnit);
            }

            return processingUnit.getInstances().length;
        }
        return 0;
    }

    /**
     * Gets a service's planned number of instances.
     *
     * @param processingUnit
     *            The processing unit implementing this service
     * @return the planned number of instances for the specified service (PU)
     */
    private int getPlannedNumberOfInstances(final ProcessingUnit processingUnit) {

        if (processingUnit == null) {
            return 0;
        }

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

    /**
     * Gets the number of RUNNING processing unit instances.
     *
     * @param processingUnit
     *            the PU to examine
     * @return the number of RUNNING processing unit instances.
     */
    private int getNumberOfUSMServicesWithRunningState(
            final ProcessingUnit processingUnit) {

        int puInstanceCounter = 0;

        if (processingUnit != null) {
            for (ProcessingUnitInstance pui : processingUnit) {
                if (isUsmStateOfPuiRunning(pui)) {
                    puInstanceCounter++;
                }
            }
        }

        return puInstanceCounter;
    }

    private boolean isUsmStateOfPuiRunning(final ProcessingUnitInstance pui) {
        USMState instanceState = getInstanceUsmState(pui);
        return (instanceState == CloudifyConstants.USMState.RUNNING);
    }

    /**
     * Gets a PU instance's USM state.
     *
     * @param pui
     *            the PU instance to examine
     * @return the USM state of the specified PU instance
     */
    private USMState getInstanceUsmState(final ProcessingUnitInstance pui) {
        final ProcessingUnitInstanceStatistics statistics = pui.getStatistics();
        if (statistics == null) {
            return null;
        }
        final Map<String, ServiceMonitors> puMonitors = statistics.getMonitors();
        if (puMonitors == null) {
            return null;
        }
        final ServiceMonitors serviceMonitors = puMonitors.get("USM");
        if (serviceMonitors == null) {
            return null;
        }
        final Map<String, Object> monitors = serviceMonitors.getMonitors();
        if (monitors == null) {
            return null;
        }

        return USMState.values()[(Integer) monitors.get(CloudifyConstants.USM_MONITORS_STATE_ID)];
    }

    private String getApplicationAuthorizationGroups(final Application application) {
        String appAuthGroups = "";
        // getting the application's authGroups from its first service,
        // assuming they all have the same authorization groups.
        final ProcessingUnit pu = application.getProcessingUnits().iterator().next();
        if (pu != null) {
            appAuthGroups = pu.getBeanLevelProperties().getContextProperties().
                    getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
        }

        return appAuthGroups;
    }

}
