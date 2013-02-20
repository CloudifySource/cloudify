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
package org.cloudifysource.dsl;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.junit.Assert;
import org.junit.Test;
/**
 * This test will open some ports and test arePortsOccupied, isPortOccupied, isPortFree, arePortsFree.
 * @author adaml
 *
 */
public class PortDetectionTest {
	
	private List<ServerSocket> serverSockets;
	
	@Test
	public void testIsPortOccupied(){
		//assert all ports are closed before test begins.
		Assert.assertTrue("port 4443 was open before test started", !ServiceUtils.isPortOccupied(4443));
		Assert.assertTrue("port 4445 was open before test started", !ServiceUtils.isPortOccupied(4445));
		Assert.assertTrue("port 4446 was open before test started", !ServiceUtils.isPortOccupied(4446));
		
		List<Integer> ports = new ArrayList<Integer>();
		ports.add(4443);
		ports.add(4445);
		ports.add(4446);
		
		//*********Open ports 4443, 4445, 4446***********
		openPorts(ports);
		
		//Test isPortOccupied:
		Assert.assertTrue("port 4443 is not open", ServiceUtils.isPortOccupied(4443));
		Assert.assertTrue("port 4445 is not open", ServiceUtils.isPortOccupied(4445));
		Assert.assertTrue("port 4446 is not open", ServiceUtils.isPortOccupied(4446));
		//Test arePortsOccupied:
		Assert.assertTrue("arePortsOccupied failed to detect all ports are occupied.", ServiceUtils.arePortsOccupied(ports));
		//Test arePortsFree:
		Assert.assertFalse("arePortsFree failed to detect all ports are open.", ServiceUtils.arePortsFree(ports));
		
		//close one of the open ports
		closePort(4445);
		
		//Test arePortsOccupied:
		Assert.assertFalse("arePortsOccupied found port 4445 is occupied.", ServiceUtils.arePortsOccupied(ports));
		//Test arePortsFree:
		Assert.assertFalse("arePortsFree failed to detect all ports are open.", ServiceUtils.arePortsFree(ports));
		
		//************Close all ports opened.***********
		closeAllPorts();
		
		//Test arePortsOccupied:
		Assert.assertFalse("arePortsOccupied detected that some ports are still occupied.", ServiceUtils.arePortsOccupied(ports));
		//Test arePortsFree:
		Assert.assertTrue("arePortsFree detected that some ports are still occupied.", ServiceUtils.arePortsFree(ports));
	}

	private void openPorts(List<Integer> ports) {
		
		serverSockets = new ArrayList<ServerSocket>();
		
		for (Integer port : ports) {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				//keep reference to all server sockets in-order to close them later on.
				serverSockets.add(serverSocket);
			} catch (IOException e) {
				Assert.fail("Failed to open the following port:" + port);
				e.printStackTrace();
			}
		}
	}
	
	private void closePort(int port){
		for (ServerSocket serverSocket : serverSockets) {
			if (serverSocket.getLocalPort() == port){
				try {
					serverSocket.close();
				} catch (IOException e) {
					Assert.fail("Failed to close port number " + port);
					e.printStackTrace();
				}
				break;
			}
		}
	}
	
	private void closeAllPorts(){
		if (serverSockets != null){
			for (ServerSocket serverSocket : serverSockets) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					Assert.fail("Failed to close the following port:" + serverSocket.getLocalPort());
					e.printStackTrace();
				}
			}
		}
	}
}
