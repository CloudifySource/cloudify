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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;

/***************
 * An extension of Machine Details that is safe to use with the SpaceDocumentConverter. Any properties that cannot be
 * safely serialized will be nulled out and replaced with appropriate safe fields.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class PersistentMachineDetails extends MachineDetails {

	private String keyFileName = null;

	public PersistentMachineDetails() {

	}

	/******
	 * Populates the fields of the current object from the given MachineDetails. Note that all copy operations are
	 * shallow. Unsafe properties will be replaced.
	 * 
	 * @param md
	 *            the machine detaions to copy.
	 */
	public void populateFromMachineDetails(final MachineDetails md) {
		try {
			PropertyUtils.copyProperties(this, md);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to create persistent machine details: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException("Failed to create persistent machine details: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Failed to create persistent machine details: " + e.getMessage(), e);
		}

		if (this.getKeyFile() != null) {
			this.setKeyFileName(this.getKeyFile().getAbsolutePath());
			this.setKeyFile(null);
		}

	}

	/**************
	 * Creates a new machine details object from the current persistent details. Unsafe properties are re-created.
	 * 
	 * @return the machine details.
	 **/
	public MachineDetails toMachineDetails() {
		final MachineDetails md = new MachineDetails();
		try {
			PropertyUtils.copyProperties(md, this);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to create machine details: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException("Failed to create machine details: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Failed to create machine details: " + e.getMessage(), e);
		}

		if (this.getKeyFileName() != null) {
			final File keyFile = new File(this.getKeyFileName());
			md.setKeyFile(keyFile);
		}

		return md;

	}

	public String getKeyFileName() {
		return keyFileName;
	}

	public void setKeyFileName(final String keyFileName) {
		this.keyFileName = keyFileName;
	}
}
