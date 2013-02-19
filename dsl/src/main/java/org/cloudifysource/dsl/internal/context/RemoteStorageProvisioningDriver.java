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

package org.cloudifysource.dsl.internal.context;


import java.rmi.Remote;

import org.cloudifysource.dsl.context.blockstorage.RemoteStorageOperationException;

/**
 * 
 * @author elip
 *
 */
public interface RemoteStorageProvisioningDriver extends Remote {
	
	void attachVolume(final String volumeId, String device, final String ip) throws RemoteStorageOperationException;
	
	String createVolume(final String templateName, final String locationId) throws RemoteStorageOperationException;
	
	void detachVolume(final String volumeId, String ip) throws RemoteStorageOperationException;
	
	void deleteVolume(final String location, final String volumeId) throws RemoteStorageOperationException;

}
