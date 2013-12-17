/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.validators;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.IPUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ValidateApplicationTest</code> contains tests for the class <code>{@link HostNameValidator}</code>.
 * 
 * @author noak
 * @since 2.7.0
 */
public class CloudifyMachineValidatorTest {
	
	private static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS_ENV";
	private static final String GSC_PORT_OR_RANGE = "GSC_PORT_OR_RANGE";
	private static final String GSA_PORT_OR_RANGE = "GSA_PORT_OR_RANGE";
	private static final String LUS_PORT_OR_RANGE = "LUS_PORT_OR_RANGE";
	private static final String ESM_PORT_OR_RANGE = "ESM_PORT_OR_RANGE";
	private static final String GSM_PORT_OR_RANGE = "GSM_PORT_OR_RANGE";

	private static final String GSA_JAVA_OPTIONS = "-Dcom.gs.zones=management "
			+ "-Dcom.gs.agent.auto-shutdown-enabled=true  -Xmx128m -Xms128m "
			+ "-Dcom.gs.transport_protocol.lrmi.bind-port=7002";
	private static final String LUS_JAVA_OPTIONS = "-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=4174 "
			+ "-Dcom.gs.zones=management -Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=4174 -Xmx128m -Xms128m "
			+ "-Dcom.gs.transport_protocol.lrmi.bind-port=7001";
	private static final String ESM_JAVA_OPTIONS = " -Xmx128m -Xms128m -Dcom.gs.transport_protocol.lrmi.bind-port=7003 "
			+ "-Dcom.gs.esm.discovery_polling_interval_seconds=20 ";
	private static final String GSM_JAVA_OPTIONS = "-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=4174 "
			+ "-Dgsm.excludeGscOnFailedInstance.disabled=true -Dcom.gs.zones=management "
			+ "-Dorg.jini.rio.monitor.pendingRequestDelay=1000 -Dcom.gigaspaces.start.httpPort=6666 "
			+ "-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=4174 -Xmx128m -Xms128m "
			+ "-Dcom.gs.transport_protocol.lrmi.bind-port=7000";
	
	private List<CloudifyMachineValidator> validatorsList;
	private Map<String, String> envMap = new HashMap<String, String>();
	private ServerSocket lusServerSocket = null;
	
	@Before
	public void setUp() throws Exception {
		setEnv();
		initValidatorsList();
		openLusServerSocket();
	}


	/**
	 * Run the Object doExecute() method test.
	 * @throws Exception
	 */
	@Test
	public void testAllValidators() throws Exception {
		try {
			for (CloudifyMachineValidator cloudifyMachineValidator : validatorsList) {
				cloudifyMachineValidator.validate();
			}
		} catch (Exception e) {
			Assert.fail("CloudifyMachine validation failed: " + e.getMessage());
		}
	}

	
	private void setEnv() {
		envMap.put(LUS_IP_ADDRESS_ENV, "127.0.0.1:4174");
		envMap.put(GSC_PORT_OR_RANGE, "7010-7110");
		envMap.put(GSA_PORT_OR_RANGE, parseJavaOptionsString(GSA_JAVA_OPTIONS));	// "7002"
		envMap.put(LUS_PORT_OR_RANGE, parseJavaOptionsString(LUS_JAVA_OPTIONS));	// "7001"
		envMap.put(ESM_PORT_OR_RANGE, parseJavaOptionsString(ESM_JAVA_OPTIONS));	// "7003"
		envMap.put(GSM_PORT_OR_RANGE, parseJavaOptionsString(GSM_JAVA_OPTIONS));	// "7000"
	}
	
	private void initValidatorsList() {
		validatorsList = new ArrayList<CloudifyMachineValidator>();
		
		// host name and address
		validatorsList.add(new HostNameValidator());

		// nic address server socket
		validatorsList.add(new NicAddressValidator());
		
		// lus connectivity
		LusConnectionValidator lusValidator = new LusConnectionValidator();
		lusValidator.setLusIpAddress(envMap.get(LUS_IP_ADDRESS_ENV));
		validatorsList.add(lusValidator);
		
		// portAvailability for agent
		PortAvailabilityAgentValidator portAvailabilityAgentValidator = new PortAvailabilityAgentValidator();
		portAvailabilityAgentValidator.setGscPortOrRange(envMap.get(GSC_PORT_OR_RANGE));
		portAvailabilityAgentValidator.setGsaPortOrRange(envMap.get(GSA_PORT_OR_RANGE));
		validatorsList.add(portAvailabilityAgentValidator);

		// portAvailability for management
		PortAvailabilityManagementValidator portAvailabilityManagementValidator = 
				new PortAvailabilityManagementValidator();
		portAvailabilityManagementValidator.setGscPortOrRange(envMap.get(GSC_PORT_OR_RANGE));
		portAvailabilityManagementValidator.setGsaPortOrRange(envMap.get(GSA_PORT_OR_RANGE));
		portAvailabilityManagementValidator.setLusPortOrRange(envMap.get(LUS_PORT_OR_RANGE));
		portAvailabilityManagementValidator.setEsmPortOrRange(envMap.get(ESM_PORT_OR_RANGE));
		portAvailabilityManagementValidator.setGsmPortOrRange(envMap.get(GSM_PORT_OR_RANGE));
		validatorsList.add(portAvailabilityManagementValidator);
	}
	
	
	// open a server socket on the specified port to listen to it
	private void openLusServerSocket() {
		String lusIpAddress = envMap.get(LUS_IP_ADDRESS_ENV);
		String hostAddress = IPUtils.getHostFromFullAddress(lusIpAddress);
		int port = IPUtils.getPortFromFullAddress(lusIpAddress);	
		
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(hostAddress), port);
			lusServerSocket = new ServerSocket();
			lusServerSocket.bind(socketAddress);
		} catch (Exception e) {
			// TODO noak throw exception here
		}
	}
	
	
	@After
	public void clean() {
		if (lusServerSocket != null) {
			try {
				lusServerSocket.close();
			} catch (Exception e) {
				// TODO noak handle
			}
		}
	}
	
	private static String parseJavaOptionsString(final String javaOptionsStr) {
		
		String portOrRange = "";
		
		int sysPropIndex = javaOptionsStr.indexOf(CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=");
		if (sysPropIndex == -1) {
			throw new IllegalArgumentException("javaOptionsStr is missing the system property \"" 
					+ CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "\"");
		}
		
		int startIndex = sysPropIndex + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY.length() + 1;
		int endIndex = javaOptionsStr.indexOf(" ", startIndex);
		
		if (endIndex > -1) {
			portOrRange = javaOptionsStr.substring(startIndex, endIndex);
		} else {
			portOrRange = javaOptionsStr.substring(startIndex);
		}

		if (StringUtils.isNotBlank(portOrRange)) {
			portOrRange = portOrRange.trim();
		}
		
		return portOrRange;
	}

}