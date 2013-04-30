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
package org.cloudifysource.dsl.internal.tools.download;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  This class implements the {@link org.cloudifysource.dsl.internal.tools.download.ResourceDownloadFacade}
 *  The implementation exposes a variety of methods used for downloading a resource.
 *  Facade will be initialized with a {@link org.cloudifysource.dsl.internal.tools.download.ResourceDownloader}
 *  class instance.
 *  
 * @author adaml
 * @since 2.6.0
 *
 */
public class ResourceDownloadFacadeImpl implements ResourceDownloadFacade {

	private static final Logger logger = Logger
			.getLogger(ResourceDownloadFacadeImpl.class.getName());
	
	private final long DEFAULT_DOWNLOAD_TIMEOUT_MILLIS = 600000;
	private final boolean DEFAULT_SKIP_EXISTING = true;

	
	private ResourceDownloader resourceDownloader;

	public ResourceDownloadFacadeImpl(final ResourceDownloader resourceDownloader) {
		this.resourceDownloader = resourceDownloader; 
		
	}
	
	@Override
	public void get(final String urlString, final String fileDest) 
			throws ResourceDownloadException, TimeoutException {
		get(urlString, fileDest, DEFAULT_SKIP_EXISTING, null, 
				DEFAULT_DOWNLOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
	public void get(final String urlString, final String fileDest, final boolean skipExisting) 
			throws ResourceDownloadException, TimeoutException {
		get(urlString, fileDest, skipExisting, null, 
				DEFAULT_DOWNLOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	
	private void start() throws ResourceDownloadException, TimeoutException {
		resourceDownloader.download();
	}
	
	@Override
	public void get(final String urlString, final String fileDest, final boolean skipExisting,
			final String hashUrl) throws ResourceDownloadException, TimeoutException {
		get(urlString, fileDest, skipExisting, hashUrl, 
				DEFAULT_DOWNLOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		
	}

	@Override
	public void get(final String urlString, final String fileDest, final boolean skipExisting,
			final long timeout, final TimeUnit unit) throws ResourceDownloadException, TimeoutException {
		get(urlString, fileDest, skipExisting, null, timeout, unit);
		
	}
	
	@Override
	public void get(final String urlString, final String fileDest, final boolean skipExisting,
			final String hashUrl, final long timeout, final TimeUnit unit)
					throws ResourceDownloadException, TimeoutException {
		logger.log(Level.INFO, "Starting download from " + urlString);
		initRecourceDownloader(urlString, fileDest, skipExisting, hashUrl, timeout, unit);
		start();
	}
	
	private void initRecourceDownloader(final String urlString, final String fileDest, 
			final boolean skipExisting, final String hashUrl, final long timeout, final TimeUnit unit) 
					throws ResourceDownloadException {
		
		URL downloadUrl = null;
		URL hashDownloadUrl = null;
		File fileDestination = new File(fileDest);
		try {
			downloadUrl = new URL(urlString);
			if (hashUrl != null) {
				hashDownloadUrl = new URL(hashUrl);
			}
		} catch (MalformedURLException e) {
			throw new ResourceDownloadException("Failed initializing resource downloader.", e);
		}
		long timeoutInMillis = unit.toMillis(timeout);
		
		this.resourceDownloader.setUrl(downloadUrl);
		this.resourceDownloader.setHashUrl(hashDownloadUrl);
		this.resourceDownloader.setResourceDest(fileDestination);
		this.resourceDownloader.setTimeoutInMillis(timeoutInMillis);
		this.resourceDownloader.setSkipExisting(skipExisting);
	}
}
