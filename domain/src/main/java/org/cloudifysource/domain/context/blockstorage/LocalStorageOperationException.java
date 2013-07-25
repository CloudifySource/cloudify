/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.domain.context.blockstorage;

/**
 * Exception used when failures occur during a local storage operation.
 * @see {@link VolumeUtils#formatAndMount(String, String, String)}
 * @author elip
 *
 */
public class LocalStorageOperationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LocalStorageOperationException(final String message) {
		super(message);
	}

	public LocalStorageOperationException(final Exception e) {
		super(e);
	}

	public LocalStorageOperationException(final String message, final Exception e) {
		super(message, e);
	}

}
