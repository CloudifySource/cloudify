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
		envMap.put("LRMI_PORT_RANGE", "7010-7110");
	}
	
	private void initValidatorsList() {
		validatorsList = new ArrayList<CloudifyMachineValidator>();
		validatorsList.add(new HostNameValidator());
		validatorsList.add(new NicAddressValidator());
		validatorsList.add(new LusConnectionValidator(envMap.get(LUS_IP_ADDRESS_ENV)));
		validatorsList.add(new PortAvailabilityValidator(envMap.get("LRMI_PORT_RANGE")));
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

}