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

import static com.gigaspaces.log.LogEntryMatchers.regex;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.EventLogConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.util.LifecycleEventsContainer.PollingState;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.zone.Zone;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;

/**
 * the RestPollingRunnable provides a service installation polling mechanism 
 * for lifecycle and instance count changes events.
 * the events will be saved in a dedicated LifecycleEventsContainer that 
 * will be sampled by the client.
 * 
 * Initialize the Runnable with service names their planned number of instances. 
 *   
 * @author adaml
 *
 */
public class RestPollingRunnable implements Runnable {

    private static final int UNINSTALL_POLLING_INTERVAL = 2000;

    // a map containing all of the application services and their planned number
    // of instances.
    // The services are ordered according to the installation order defined by
    // the application.
    private final LinkedHashMap<String, Integer> serviceNames;

    private final String applicationName;

    private final int FIVE_MINUTES_MILLISECONDS = 60 * 1000 * 5;

    private Admin admin;

    private long endTime;

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.{0}\\].*";

    private boolean isUninstall = false;

    private LifecycleEventsContainer lifecycleEventsContainer;

    private boolean isServiceInstall;

    private static final Logger logger = Logger.getLogger(RestPollingRunnable.class.getName());

    /**
     * Create a rest polling runnable to poll for a specific service's installation
     * lifecycle events with the application name set to the "default" application name. 
     * 
     * Use this constructor if polling a single service installation.
     * 
     * @param serviceName
     *            the service name to deploy
     * @param timeout timeout polling timeout.
     * @param plannedNumberOfInstances the planned number of instances.
     * @param timeunit polling timeout timeunit.
     */
    public RestPollingRunnable(final String applicationName,
            final long timeout, final TimeUnit timeunit) {

        this.serviceNames = new LinkedHashMap<String, Integer>();
        this.applicationName = applicationName;
    }

    /**
     * sets the admin.
     * @param admin admin instance.
     */
    public void setAdmin(final Admin admin) {
        this.admin = admin;
    }

    /**
     * sets the current lifecycleEventsContainer to be updated 
     * by the callable task.
     * @param lifecycleEventsContainer ref to a lifecycleEventsContainer.
     */
    public void setLifecycleEventsContainer(
            final LifecycleEventsContainer lifecycleEventsContainer) {
        this.lifecycleEventsContainer = lifecycleEventsContainer;
    }

    /**
     * Add a service to the polling callable. the service will be sampled 
     * until it reaches it's planned number of instances or until a timeout
     * exception is thrown.
     * @param serviceName The absoulute pu name.
     * @param plannedNumberOfInstances planned number of instances.
     */
    public void addService(final String serviceName, final int plannedNumberOfInstances) {
        this.serviceNames.put(serviceName, plannedNumberOfInstances);
    }

    /**
     * sets the services to poll and their planned number of instances.
     * @param isServiceInstall true if installation is for a single service.
     */
    public void setIsServiceInstall(final boolean isServiceInstall) {
        this.isServiceInstall = isServiceInstall;
    }

    public void setEndTime(final long timeout, final TimeUnit timeunit) {
        this.endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
    }


    @Override
    public void run() {
        
        try {
        if (this.serviceNames.isEmpty()) {
            logger.log(Level.INFO, "Polling Polling for lifecycle events has ended successfully." 
                    + " Terminating the polling task");
            this.lifecycleEventsContainer.setPollingState(PollingState.ENDED);
            //We stop the thread from being scheduled again.
            throw new RuntimeException("Polling has ended successfully");
        } 
            
            if (System.currentTimeMillis() > this.endTime) {
                throw new TimeoutException("Timed out");
            }

            pollForLogs();

        } catch (Throwable e) {
            logger.log(Level.INFO, "Polling task terminated. Reason: " + e.getMessage());
            this.lifecycleEventsContainer.setExecutionException(e);
            //this exception should not be caught. it is meant to make the scheduler stop
            //the thread execution.
            throw new RuntimeException(e);

        } 

    }

    /**
     * Goes over each service defined prior to the callable execution 
     * and polls it for lifecycle and instance count events.
     */
    private void pollForLogs() {

        LinkedHashMap<String, Integer> serviceNamesClone = new LinkedHashMap<String, Integer>();
        serviceNamesClone.putAll(this.serviceNames);

        for (Map.Entry<String, Integer> entry : serviceNamesClone.entrySet()) {

            addServiceLifecycleLogs(entry);

            addServiceInstanceCountEvents(entry);
        }
    }

    private void addServiceLifecycleLogs(Entry<String, Integer> entry) {
        List<Map<String, String>> servicesLifecycleEventDetailes;
        servicesLifecycleEventDetailes = new ArrayList<Map<String, String>>();
        String serviceName = entry.getKey();
        final String absolutePuName = ServiceUtils.getAbsolutePUName(
                this.applicationName, serviceName);
        logger.log(Level.FINE, 
                "Polling for lifecycle events on service: " + absolutePuName);
        final Zone zone = admin.getZones().getByName(absolutePuName);
        if (zone == null) {
            logger.log(Level.FINE, "Zone " + absolutePuName + " does not exist");
            if (isUninstall) {
                logger.log(Level.INFO, 
                        "Polling for service " + absolutePuName + " has ended successfully");
                this.serviceNames.remove(serviceName);
            }
            return;
        }
        //TODO: this is not very efficient. Maybe possible to move the LogEntryMatcher
        //as field add to init to call .
        final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME,
                absolutePuName);
        final LogEntryMatcher matcher = regex(regex);
        for (final GridServiceContainer container : zone
                .getGridServiceContainers()) {
            logger.log(Level.FINE, 
                    "Polling GSC with uid: " + container.getUid());
            final LogEntries logEntries = container.logEntries(matcher);
            //Get lifecycle events.
            for (final LogEntry logEntry : logEntries) {
                if (logEntry.isLog()) {
                    final Date fiveMinutesAgoGscTime = new Date(
                            new Date().getTime()
                            + container.getOperatingSystem()
                            .getTimeDelta()
                            - FIVE_MINUTES_MILLISECONDS);
                    if (fiveMinutesAgoGscTime.before(new Date(logEntry
                            .getTimestamp()))) {
                        final Map<String, String> serviceEventsMap = getEventDetailes(
                                logEntry, container, absolutePuName);
                        servicesLifecycleEventDetailes.add(serviceEventsMap);
                    }
                }
            }

            this.lifecycleEventsContainer.addLifecycleEvents(servicesLifecycleEventDetailes);
        }
    }

    private void addServiceInstanceCountEvents(Entry<String, Integer> entry) {

        String serviceName = entry.getKey();
        String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
        int plannedNumberOfInstances = getPlannedNumberOfInstances(serviceName);
        int numberOfServiceInstances = getNumberOfServiceInstances(absolutePuName);

        if (numberOfServiceInstances == 0) {
            if (!isUninstall) {
                this.lifecycleEventsContainer.addInstanceCountEvent("Deploying " + serviceName + " with " 
                        + plannedNumberOfInstances + " planned instances.");
            }
        } else {
            this.lifecycleEventsContainer.addInstanceCountEvent("[" 
                    + serviceName 
                    + "] " + "Deployed "
                    + numberOfServiceInstances
                    + " planned "
                    + plannedNumberOfInstances);
        }

        if (plannedNumberOfInstances ==  numberOfServiceInstances) {
            if (!isServiceInstall) {
                if (!isUninstall) {
                    this.lifecycleEventsContainer.addInstanceCountEvent("Service \"" + serviceName 
                            + "\" successfully installed (" + numberOfServiceInstances + " Instances)");
                    this.serviceNames.remove(serviceName);
                } 
            } else {
                this.serviceNames.remove(serviceName);
            }
        }

        final Zone zone = admin.getZones().getByName(absolutePuName);
        if (zone == null) {
            logger.log(Level.FINE, "Zone " + absolutePuName + " does not exist");
            if (isUninstall) {
                this.lifecycleEventsContainer.addInstanceCountEvent(
                        "Undeployed service " + serviceName + ".");
                this.lifecycleEventsContainer.addInstanceCountEvent("Service \"" + serviceName 
                        + "\" uninstalled successfully");
            }
        }
    }

    /**
     * The planned number of service instances is saved to the serviceNames map
     * during initialization of the callable.
     * in case of datagrid deployment, the planned number of instances will be 
     * reviled only after it's PU has been created and so we need to poll the pu
     * to get the correct number of planned instances in case the pu is of type datagrid.
     * 
     * @param serviceName The service name
     * @return planned number of service instances
     */
    private int getPlannedNumberOfInstances(final String serviceName) {
        if (isUninstall) {
            return 0;
        }
        String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePuName);
        if (processingUnit != null) {
            Map<String, String> elasticProperties = ((DefaultProcessingUnit) processingUnit).getElasticProperties();
            if (elasticProperties.containsKey("schema")) {
                String clusterSchemaValue = elasticProperties.get("schema");
                if ("partitioned-sync2backup".equals(clusterSchemaValue)) {
                    return processingUnit.getTotalNumberOfInstances();
                }
            }
        }

        if (serviceName.contains(serviceName)) {
            return serviceNames.get(serviceName);
        }
        throw new IllegalStateException("Service planned number of instances is undefined");

    }

    /**
     *  Gets the service instance count for every type of
     *  service (USM/Other). if the service pu is not running
     *  yet, returns 0.
     *  
     * @param absolutePuName The absolute service name.
     * @return the service's number of running instances.
     */
    private int getNumberOfServiceInstances(final String absolutePuName) {

        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePuName);

        if (processingUnit != null) {
            if (processingUnit.getType().equals(
                    ProcessingUnitType.UNIVERSAL)) {
                return getNumberOfUSMServicesWithRunningState(absolutePuName);
            }

            return admin.getProcessingUnits()
                    .getProcessingUnit(absolutePuName).getInstances().length;
        }
        return 0;
    }

    // returns the number of RUNNING processing unit instances.
    private int getNumberOfUSMServicesWithRunningState(
            final String absolutePUName) {

        int puiInstanceCounter = 0;
        ProcessingUnit processingUnit = admin.getProcessingUnits()
                .getProcessingUnit(absolutePUName);

        for (ProcessingUnitInstance pui : processingUnit) {
            // TODO: get the instanceState step
            int instanceState = (Integer) pui.getStatistics().getMonitors()
                    .get("USM").getMonitors()
                    .get(CloudifyConstants.USM_MONITORS_STATE_ID);
            if (CloudifyConstants.USMState.values()[instanceState]
                    .equals(CloudifyConstants.USMState.RUNNING)) {
                puiInstanceCounter++;
            }
        }
        return puiInstanceCounter;
    }

    /**
     * tells the polling task to expect uninstall or install of service.
     * the default value is set to false.
     * @param isUninstall is the task being preformed an uninstall task.
     */
    public void setIsUninstall(final boolean isUninstall) {
        this.isUninstall = isUninstall; 
    }

    /**
     * generates a map containing all of the event's details.
     * @param logEntry The event log entry originated from the GSC log
     * @param container the GSC of the specified event
     * @param absolutePuName the absolute processing unit name.
     * @return returns a details map containing all of an events details.
     */
    private Map<String, String> getEventDetailes(final LogEntry logEntry,
            final GridServiceContainer container, final String absolutePuName) {

        final Map<String, String> returnMap = new HashMap<String, String>();

        returnMap.put(EventLogConstants.getTimeStampKey(),
                Long.toString(logEntry.getTimestamp()));
        returnMap.put(EventLogConstants.getMachineHostNameKey(), container
                .getMachine().getHostName());
        returnMap.put(EventLogConstants.getMachineHostAddressKey(), container
                .getMachine().getHostAddress());
        returnMap.put(EventLogConstants.getServiceNameKey(),
                ServiceUtils.getApplicationServiceName(absolutePuName,
                        this.applicationName));
        // The string replacement is done since the service name that is
        // received from the USM logs derived from actual PU name.
        returnMap.put(
                EventLogConstants.getEventTextKey(),
                logEntry.getText().replaceFirst(
                        absolutePuName + "-",
                        returnMap.get(EventLogConstants.getServiceNameKey())
                        + "-"));

        return returnMap;
    }


}
