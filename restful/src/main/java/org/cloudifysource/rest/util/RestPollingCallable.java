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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.EventLogConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
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
 * the RestPollingCallable provides a service installation polling mechanism 
 * for lifecycle and instance count changes events.
 * the events will be saved in a dedicated LifecycleEventsContainer that 
 * will be sampled by the client.
 *   
 * @author adaml
 *
 */
public class RestPollingCallable implements Callable<Boolean> {

    private Admin admin;

    private long endTime;

    private int pollingInterval;

    private final int DEFAULT_POLLING_INTERVAL = 4000;

    private static final int FIVE_MINUTES_MILLISECONDS = 60 * 1000 * 5;

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.{0}\\].*";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final String applicationName;

    private boolean isUninstall = false;

    // a map containing all of the application services and their planned number
    // of instances.
    // The services are ordered according to the installation order defined by
    // the application.
    private final LinkedHashMap<String, Integer> serviceNames;

    private LifecycleEventsContainer lifecycleEventsContainer;

    private boolean isServiceInstall;

    /**
     * Create a rest polling runnable to poll for a specific application's installation
     *  
     *  Use this constructor if polling a single service installation.
     *  
     * @param application the application to deploy.
     * @param timeout polling timeout.
     * @param timeunit polling timeout timeunit.
     */
    public RestPollingCallable(Application application,
            final long timeout, final TimeUnit timeunit) {
        
        this.isServiceInstall  = false;
        this.serviceNames = new LinkedHashMap<String, Integer>();
        this.applicationName = application.getName();
        for (Service service : application.getServices()) {
            this.serviceNames.put(service.getName(), service.getNumInstances());
        }

        this.endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
        this.pollingInterval = DEFAULT_POLLING_INTERVAL;
    }

    /**
     * Create a rest polling runnable to poll for a specific service's installation
     * lifecycle events with the application name set to the "default" application name. 
     * 
     * Use this constructor if polling a single service installation.
     * 
     * @param service
     *            the service to deploy
     * @param timeout timeout polling timeout.
     * @param timeunit polling timeout timeunit.
     */
    public RestPollingCallable(String serviceName, int plannedNumberOfInstances, final long timeout,
            final TimeUnit timeunit) {

        this.isServiceInstall  = true;
        this.serviceNames = new LinkedHashMap<String, Integer>();
        this.serviceNames.put(serviceName, plannedNumberOfInstances);
        this.applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
        this.pollingInterval = DEFAULT_POLLING_INTERVAL;
        this.endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
    }

    @Override
    public Boolean call() throws Exception {
        logger.info("Starting poll for lifecycle events on services: " + this.serviceNames.toString());
        waitForServiceInstanceAndLifecycleEvents();
        logger.info("Polling for lifecycle events ended successfully");
        return true;

    }
    
    /**
     * goes over all available GSC's and scans their logs for new lifecycle events.
     * In each iteration it will updated the lifecycle event container sheared resource.
     * This method will only retrieve lifecycle events that occurred in the last 5 minutes.
     * @return a list of Maps containing all events and their details.
     * @throws InterruptedException 
     * @throws TimeoutException 
     */
    private void waitForServiceInstanceAndLifecycleEvents() throws InterruptedException, TimeoutException {
        
        List<Map<String, String>> servicesLifecycleEventDetailes;
        while (System.currentTimeMillis() < this.endTime) {
            servicesLifecycleEventDetailes = new ArrayList<Map<String, String>>();
            Iterator<Map.Entry<String, Integer>> entryIterator = serviceNames.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Integer> entry = entryIterator.next();
                String serviceName = entry.getKey();
                final String absolutePuName = ServiceUtils.getAbsolutePUName(
                        this.applicationName, serviceName);
                final Zone zone = admin.getZones().getByName(absolutePuName);
                if (zone == null) {
                    logger.finer("Zone " + absolutePuName + " does not exist");
                    continue;
                }
                int plannedNumberOfInstances = getPlannedNumberOfInstances(serviceName);
                //TODO: this is not very efficient. Maybe possible to move the LogEntryMatcher
                //as field add to init to call .
                final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME,
                        absolutePuName);
                final LogEntryMatcher matcher = regex(regex);
                int numberOfServiceInstances = 0;
                for (final GridServiceContainer container : zone
                        .getGridServiceContainers()) {
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
                                final Map<String, String> serviceEventsMap = getServiceDetailes(
                                        logEntry, container, absolutePuName);
                                servicesLifecycleEventDetailes.add(serviceEventsMap);
                            }
                        }
                    }
                    this.lifecycleEventsContainer.addLifecycleEvents(servicesLifecycleEventDetailes);
                    numberOfServiceInstances = getNumberOfServiceInstances(absolutePuName);
                    if (numberOfServiceInstances == 0){
                        this.lifecycleEventsContainer.addInstanceCountEvent("Deploying " + serviceName + " with " + plannedNumberOfInstances + " planned instances.");
                    }else{
                        this.lifecycleEventsContainer.addInstanceCountEvent("[" + ServiceUtils.getApplicationServiceName(absolutePuName, this.applicationName) 
                            + "] " + "Deployed "
                            + numberOfServiceInstances
                            + " of "
                            + plannedNumberOfInstances);
                    }
                }
                if (plannedNumberOfInstances ==  numberOfServiceInstances){
                    if (!isServiceInstall){
                        this.lifecycleEventsContainer.addInstanceCountEvent("Service \"" + serviceName + "\" successfully installed (" + numberOfServiceInstances + " Instances)");
                    }
                    entryIterator.remove();
                }
                if (serviceNames.isEmpty()){
                    return;
                }
            }
            Thread.sleep(pollingInterval);
        }
        throw new TimeoutException();
    }

    /**
     * The planned number of service instances is saved to the serviceNames map
     * during initialization of the callable.
     * in case of datagrid deployment, the planned number of instances will be 
     * reviled only after it's PU has been created and so we need to poll the pu
     * to get the correct number of planned instances in case the pu is of type datagrid.
     * 
     * @param serviceName
     * @return
     */
    private int getPlannedNumberOfInstances(String serviceName) {
        String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePuName);
        if (processingUnit != null){
            Map<String, String> elasticProperties = ((DefaultProcessingUnit)processingUnit).getElasticProperties();
            if (elasticProperties.containsKey("schema")){
                String ClusterSchemaValue = elasticProperties.get("schema");
                if ("partitioned-sync2backup".equals(ClusterSchemaValue)){
                    return processingUnit.getTotalNumberOfInstances();
                }
            }
        }
        return serviceNames.get(serviceName);
    }

    /**
     *  Gets the service instance count for every type of
     *  service (USM/Other). if the service pu is not running
     *  yet, returns 0.
     *  
     * @param absolutePuName
     * @return the service's number of running instances.
     */
    private int getNumberOfServiceInstances(String absolutePuName){

        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePuName);

        if (processingUnit != null) {
            if (processingUnit.getType().equals(
                    ProcessingUnitType.UNIVERSAL)) {
                return getNumberOfUSMServicesWithRunningState(absolutePuName);
            }
            return admin.getProcessingUnits()
            .getProcessingUnit(absolutePuName).getNumberOfInstances();

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
    
    public void setIsUninstall(boolean isUninstall){
        this.isUninstall = isUninstall; 
        //GSCs will disappear quickly. decrement polling interval
        this.pollingInterval = 1000;
    }

    /**
     * generates a map containing all of the event's details.
     * @param logEntry The event log entry originated from the GSC log
     * @param container the GSC of the specified event
     * @param absolutePuName the absolute processing unit name.
     * @return
     */
    private Map<String, String> getServiceDetailes(final LogEntry logEntry,
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

    public void setAdmin(Admin admin){
        this.admin = admin;
    }

    /**
     * sets the current lifecycleEventsContainer to be updated 
     * by the callable task.
     * @param lifecycleEventsContainer ref to a lifecycleEventsContainer.
     */
    public void setLifecycleEventsContainer(
            LifecycleEventsContainer lifecycleEventsContainer) {
        this.lifecycleEventsContainer = lifecycleEventsContainer;
    }

    /**
     * Sets the polling interval for the GSCs log files. 
     * It is recommended to decrease when polling an uninstall command
     * as the GSCs might shut down logging will not be done in time.
     * 
     * DEFAULT_POLLING_INTERVAL = 2000 ms.
     * 
     * @param pollingInterval
     */
    public void setPollingInterval(int pollingInterval){
        this.pollingInterval = pollingInterval;
    }

}
