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
package org.cloudifysource.dsl.internal.tools.download;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * an interface for a download facade that defines a set of doGet commands.
 * 
 * @author adaml
 * @since 2.6.0
 *
 */
public interface ResourceDownloadFacade {
	
	/**
	 * perform a get on a file from URL.
	 *  
	 * @param urlString
	 * 			file URL as string.
	 * @param fileDest
	 * 			destination file.
	 * @throws ResourceDownloadException 
	 * @throws TimeoutException 
	 */
	void get(final String urlString, final String fileDest) 
			throws ResourceDownloadException, TimeoutException;
	
	/**
	 * performs a get on a file from URL.
	 * 
	 * @param urlString
	 * 			file URL as string.
	 * @param fileDest
	 * 			destination file.
	 * @param skipExisting
	 * 			skip download if file exists. 
	 * @throws ResourceDownloadException 
	 * @throws TimeoutException 
	 */
	void get(final String urlString, final String fileDest, final boolean skipExisting)
			throws ResourceDownloadException, TimeoutException;
	
	/**
	 * performs a get on a file from URL and authenticate using checksum.
	 * 
	 * @param urlString
	 * 			file URL as string.
	 * @param fileDest
	 * 			destination file.
	 * @param skipExisting
	 * 			skip download if file exists.
	 * @param hashUrl
	 * 			hash checksum file URL as string.
	 * @throws ResourceDownloadException
	 * 			if download failed.
	 * @throws TimeoutException 
	 */
	void get(final String urlString, final String fileDest, final boolean skipExisting, 
			String hashUrl) throws ResourceDownloadException, TimeoutException;
	
	/**
	 * performs a get on a file from URL and authenticate using checksum.
	 * 
	 * @param urlString
	 * 			file URL as string.
	 * @param fileDest
	 * 			destination file.
	 * @param skipExisting
	 * 			skip download if file exists.
	 * @param hashUrl
	 * 			hash validation file url.
	 * @param timeout
	 * 			execution timeout.
	 * @param unit
	 * 			time unit.
	 * @throws ResourceDownloadException
	 * 			if download failed.
	 * @throws TimeoutException 
	 */
	void get(final String urlString, final String fileDest, final boolean skipExisting, 
			 final String hashUrl, final long timeout, final TimeUnit unit) 
					 throws ResourceDownloadException, TimeoutException;
	
	/**
	 * performs a get on a file from URL and authenticate using checksum
	 * and an existing hash file.
	 * 
	 * @param urlString
	 * 			file URL as string.
	 * @param fileDest
	 * 			destination file.
	 * @param skipExisting
	 * 			skip download if file exists.
	 * @param timeout
	 * 			execution timeout.
	 * @param unit
	 * 			time unit.
	 * @throws ResourceDownloadException
	 * 			if download failed.
	 * @throws TimeoutException 
	 */
	void get(final String urlString, final String fileDest, final boolean skipExisting,   
			final long timeout, final TimeUnit unit) throws ResourceDownloadException, TimeoutException;
}
