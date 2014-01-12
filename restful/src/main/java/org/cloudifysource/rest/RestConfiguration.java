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
package org.cloudifysource.rest;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.rest.util.RestPollingRunnable;
import org.cloudifysource.security.CustomPermissionEvaluator;
import org.cloudifysource.utilitydomain.data.CloudConfigurationHolder;
import org.openspaces.admin.Admin;
import org.openspaces.core.GigaSpace;

/**
 *
 * @author yael
 *
 */
public class RestConfiguration {

    private static final int THREAD_POOL_SIZE = 20;
    private GigaSpace gigaSpace;
    private Admin admin;
    private Cloud cloud = null;
    private CloudConfigurationHolder cloudConfigurationHolder;
    private File cloudConfigurationDir;
    private String defaultTemplateName;
    private ComputeTemplate managementTemplate;
    private String managementTemplateName;
    private final AtomicInteger lastTemplateFileNum = new AtomicInteger(0);
    private File restTempFolder;
	private CustomPermissionEvaluator permissionEvaluator;
	private File additionalTemplatesFolder;
	private List<String> cloudDeclaredTemplates;

	/**
     * A set containing all of the executed lifecycle events. used to avoid duplicate prints.
     */
    private final Set<String> eventsSet = new HashSet<String>();
    private final Map<UUID, RestPollingRunnable> lifecyclePollingThreadContainer =
            new ConcurrentHashMap<UUID, RestPollingRunnable>();
    private final ScheduledExecutorService scheduledExecutor = Executors
            .newScheduledThreadPool(10, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(final Runnable r) {
                    final Thread thread = new Thread(r,
                            "LifecycleEventsPollingExecutor-"
                                    + threadNumber.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    // Set up a small thread pool with daemon threads.
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(final Runnable r) {
                    final Thread thread = new Thread(r,
                            "ServiceControllerExecutor-"
                                    + threadNumber.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });

	public GigaSpace getGigaSpace() {
		return gigaSpace;
	}

	public void setGigaSpace(final GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}
	
    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(final Admin admin) {
        this.admin = admin;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(final Cloud cloud) {
        this.cloud = cloud;
    }

    public CloudConfigurationHolder getCloudConfigurationHolder() {
        return cloudConfigurationHolder;
    }

    public void setCloudConfigurationHolder(final CloudConfigurationHolder cloudConfigurationHolder) {
        this.cloudConfigurationHolder = cloudConfigurationHolder;
    }

    public File getCloudConfigurationDir() {
        return cloudConfigurationDir;
    }

    public void setCloudConfigurationDir(final File cloudConfigurationDir) {
        this.cloudConfigurationDir = cloudConfigurationDir;
    }

    public String getDefaultTemplateName() {
        return defaultTemplateName;
    }

    public void setDefaultTemplateName(final String defaultTemplateName) {
        this.defaultTemplateName = defaultTemplateName;
    }

    public ComputeTemplate getManagementTemplate() {
        return managementTemplate;
    }

    public void setManagementTemplate(final ComputeTemplate managementTemplate) {
        this.managementTemplate = managementTemplate;
    }

    public String getManagementTemplateName() {
        return managementTemplateName;
    }

    public void setManagementTemplateName(final String managementTemplateName) {
        this.managementTemplateName = managementTemplateName;
    }

    public AtomicInteger getLastTemplateFileNum() {
        return lastTemplateFileNum;
    }

    public File getRestTempFolder() {
        return restTempFolder;
    }

    public void setRestTempFolder(final File restTempFolder) {
        this.restTempFolder = restTempFolder;
    }

    public Set<String> getEventsSet() {
        return eventsSet;
    }

    public Map<UUID, RestPollingRunnable> getLifecyclePollingThreadContainer() {
        return lifecyclePollingThreadContainer;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

	public CustomPermissionEvaluator getPermissionEvaluator() {
		return permissionEvaluator;
	}

	public void setPermissionEvaluator(final CustomPermissionEvaluator permissionEvaluator) {
		this.permissionEvaluator = permissionEvaluator;
	}

	public File getAdditionalTempaltesFolder() {
		return additionalTemplatesFolder;
	}

	public void setAdditionalTemplatesFolder(final File additionalTemplatesFolder) {
		this.additionalTemplatesFolder = additionalTemplatesFolder;
	}

	public List<String> getCloudDeclaredTemplates() {
		return cloudDeclaredTemplates;
	}

	public void setCloudDeclaredTemplates(final List<String> cloudDeclaredTemplates) {
		this.cloudDeclaredTemplates = cloudDeclaredTemplates;
	}

}
