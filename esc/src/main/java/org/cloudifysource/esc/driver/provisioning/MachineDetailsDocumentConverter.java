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
package org.cloudifysource.esc.driver.provisioning;

import com.gigaspaces.document.SpaceDocument;

/*************
 * Converts Machine Details to Space documents (and back). Used by the Cloudify adapter.
 * @author barakme
 * @since 2.7.0
 *
 */
public class MachineDetailsDocumentConverter {

	/***********
	 * Creates a space document from a Machine details object.
	 * @param md the machine details.
	 * @return the space document.
	 */
	public SpaceDocument toDocument(final MachineDetails md) {
		if (md == null) {
			return null;
		}

		final SpaceDocument document = com.gigaspaces.document.DocumentObjectConverter.instance().toSpaceDocument(md);

		return document;
	}

	/***********
	 * Creates a machine details object from a space document.
	 * @param document the space document.
	 * 
	 * @return the MachineDetails object.
	 * @throws IllegalStateException if the object retrieved from the document is not a MachineDetails object.
	 */
	public MachineDetails toMachineDetails(final SpaceDocument document) throws IllegalStateException {

		if (document == null) {
			return null;
		}

		final Object mdObject = com.gigaspaces.document.DocumentObjectConverter.instance().toObject(document);
		if (mdObject == null) {
			return null;
		}
		if (!(mdObject instanceof MachineDetails)) {
			throw new IllegalStateException(
					"Unsupported object received in failed agent context. Was expecting MachineDetails but got: "
							+ mdObject.getClass().getName());
		}

		final MachineDetails md = (MachineDetails) mdObject;

		return md;
	}
}
