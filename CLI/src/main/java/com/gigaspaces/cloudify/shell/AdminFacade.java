/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gigaspaces.cloudify.shell;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.restclient.InvocationResult;

import com.gigaspaces.cloudify.shell.commands.CLIException;


/**
 * @author rafi
 * @since 8.0.3
 */
public interface AdminFacade {

    String install(String applicationName, File file) throws CLIException;


    String installApplication(File applicationFile,String applicationName) throws CLIException;

    String installElastic(File file,String applicationName, String serviceName, String zone, Properties props, final String templateName) throws CLIException;
    
    String startService(String applicationName, File service) throws CLIException;

    void connect(String user, String password, String url) throws CLIException;

    void disconnect() throws CLIException;

    List<String> getApplicationsList() throws CLIException;

    List<String> getServicesList(String applicationName) throws CLIException;

    void addInstance(String applicationName, String serviceName, int timeout) throws CLIException;

    void removeInstance(String applicationName, String serviceName, int instanceId) throws CLIException;

    void restart(ComponentType componentType, String componentName, Set<Integer> componentIDs) throws CLIException;

    boolean isConnected() throws CLIException;

    void undeploy(String applicationName, String serviceName) throws CLIException;

    Map<String, Object> getInstanceList(String applicationName, String serviceName) throws CLIException;
    
    Map<String, InvocationResult> invokeServiceCommand(String applicationName, String serviceName, String beanName, String commandName, Map<String, String> paramsMap) throws CLIException;
    
    InvocationResult invokeInstanceCommand(String applicationName, String serviceName, String beanName, int instanceId, String commandName, Map<String, String> paramsMap) throws CLIException;

	List<String> getMachines() throws CLIException;

	void uninstallApplication(String applicationName) throws CLIException, CLIException;
	
	boolean waitForServiceInstances(String serviceName, String applicationName, int plannedNumberOfInstances, String timeoutErrorMessage, long timeout, TimeUnit timeunit) throws CLIException, TimeoutException, InterruptedException;
}
