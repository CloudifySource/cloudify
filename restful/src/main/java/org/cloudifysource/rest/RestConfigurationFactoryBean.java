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
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.CloudCompute;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.security.CustomPermissionEvaluator;
import org.cloudifysource.utilitydomain.data.CloudConfigurationHolder;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;
import org.openspaces.admin.Admin;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author yael
 * @since 2.6.0
 *
 */
@Component
public class RestConfigurationFactoryBean implements FactoryBean<RestConfiguration> {

	private static final int MAX_FILE_NAME_APPENDER = 99;
	private static final Logger logger = Logger.getLogger(RestConfigurationFactoryBean.class.getName());
    
    private RestConfiguration config;

    @GigaSpaceContext(name = "gigaSpace")
    private GigaSpace gigaSpace;

    @Autowired(required = true)
    protected Admin admin;

	@Autowired(required = false)
	private CustomPermissionEvaluator permissionEvaluator;
	
	@Value("${restful.temporaryFolder}")
	private String temporaryFolder;
	
    @Override
    public RestConfiguration getObject() throws Exception {
        config = new RestConfiguration();
        initRestConfiguration();
        return config;
    }

    /**
     * Initialize all needed fields in RestConfiguration.
     */
    public void initRestConfiguration() throws RestErrorException {
    	logger.warning("***** starting RestConfigurationFactoryBean.initRestConfiguration");
        logger.info("Initializing cloud configuration");
        config.setGigaSpace(gigaSpace);
        config.setAdmin(admin);
        config.setPermissionEvaluator(permissionEvaluator);
        Cloud cloud = readCloud();
        if (cloud != null) {
        	logger.warning("***** cloud is not null");
        	config.setCloud(cloud);
            initCloudTemplates();
            CloudCompute cloudCompute = cloud.getCloudCompute();
            if (cloudCompute.getTemplates().isEmpty()) {
                throw new IllegalArgumentException("No templates defined in cloud configuration!");
            }
            String defaultTemplateName = cloudCompute.getTemplates().keySet().iterator().next();
            logger.info("Setting default template name to: " + defaultTemplateName
                    + ". This template will be used for services that do not specify an explicit template");
            config.setDefaultTemplateName(defaultTemplateName);
            String managementTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
            config.setManagementTemplateName(managementTemplateName);
            config.setManagementTemplate(cloudCompute.getTemplates().get(managementTemplateName));
            logger.warning("***** calling createRestTempFolder()");
			config.setRestTempFolder(createRestTempFolder());
        } else {
            logger.info("running in local cloud mode");
        }
    }
    
	
    private File createRestTempFolder() throws RestErrorException {
    	logger.warning("***** starting createRestTempFolder()");
    	String restTempFolderName = "";
    	if (!StringUtils.isEmpty(temporaryFolder)) {
    		restTempFolderName = temporaryFolder;
		} else {
			restTempFolderName = CloudifyConstants.REST_FOLDER;
		}
    	
    	logger.warning("***** restTempFolderName is: " + restTempFolderName);
    	
    	File restTempFolder = new File(restTempFolderName);
    	
    	// if the temp folder exists (left over of an unexpected shutdown) - delete it
    	// if deletion fails - create another temp folder next to it (cloudify1, cloudify2...)
        if (restTempFolder.exists()) {
        	try {
        		logger.warning("***** attempting to delete: " + restTempFolder.getAbsolutePath());
				FileUtils.deleteDirectory(restTempFolder);
			} catch (IOException e) {
				logger.warning("failed to delete rest template folder [" + restTempFolder.getAbsolutePath() + "], "
						+ "attempting to create a new folder for this purpose");
				e.printStackTrace();
				
				try {
					logger.warning("***** calling createUniqueFolderName() with parent: " 
				+ restTempFolder.getParentFile().getAbsolutePath() + " and folder name: " + restTempFolder.getName());
					String uniqueFolderName = org.cloudifysource.esc.util.FileUtils.createUniqueFolderName(
							restTempFolder.getParentFile(), restTempFolder.getName(), MAX_FILE_NAME_APPENDER);
					logger.warning("***** uniqueFolderName is: " + uniqueFolderName);
					restTempFolder = new File(restTempFolder.getParentFile(), uniqueFolderName);
				} catch (IOException ioe) {
					//TODO noak: improve this error handling?
					logger.warning("***** failed to create unique folder name, IOE message is: "
							+ ioe.getMessage());
					ioe.printStackTrace();
	    			throw new RestErrorException(
	    					CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName(), ioe.getMessage());
				}
			}
        }
        
        logger.warning("***** attempting to create the new folder");
        restTempFolder.deleteOnExit();
        final boolean mkdirs = restTempFolder.mkdirs();
        final String absolutePath = restTempFolder.getAbsolutePath();
        
        if (mkdirs) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.log(Level.INFO, "created rest temp directory - " + absolutePath);
			}
		} else {
			if (logger.isLoggable(Level.WARNING)) {
				logger.warning("failed to create rest temp directory at " + absolutePath);
			}
			throw new RestErrorException(
					CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName(), absolutePath);
		}
        
        
        return restTempFolder;
	}

	private Cloud readCloud() {
        logger.info("Loading cloud configuration");

        CloudConfigurationHolder cloudConfigurationHolder = getCloudConfigurationFromManagementSpace();
        logger.info("Cloud Configuration: " + cloudConfigurationHolder);
        final String cloudConfigurationFilePath = cloudConfigurationHolder.getCloudConfigurationFilePath();
        if (cloudConfigurationFilePath == null) {
            // must be local cloud or azure
            return null;
        }
        Cloud cloud = null;
        try {
            final File cloudConfigurationFile = new File(cloudConfigurationFilePath);
            File cloudConfigurationDir = cloudConfigurationFile.getParentFile();
            cloud = ServiceReader.readCloud(cloudConfigurationFile);
            config.setCloudConfigurationDir(cloudConfigurationDir);
            config.setCloudConfigurationHolder(cloudConfigurationHolder);
        } catch (final DSLException e) {
            throw new IllegalArgumentException(
                    "Failed to read cloud configuration file: " + cloudConfigurationHolder
                            + ". Error was: " + e.getMessage(), e);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    "Failed to read cloud configuration file: " + cloudConfigurationHolder
                            + ". Error was: " + e.getMessage(), e);
        }
        logger.info("Successfully loaded cloud configuration file from management space");
        return cloud;
    }

    private void initCloudTemplates() {
        final File additionalTemplatesFolder = new File(config.getCloudConfigurationDir(),
                CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
        logger.info("initCloudTemplates - Adding templates from folder: "
                + additionalTemplatesFolder.getAbsolutePath());
        if (!additionalTemplatesFolder.exists()) {
            logger.info("initCloudTemplates - no templates to add from folder: "
                    + additionalTemplatesFolder.getAbsolutePath());
            return;
        }
        File[] listFiles = additionalTemplatesFolder.listFiles();
        ComputeTemplatesReader reader = new ComputeTemplatesReader();
        List<ComputeTemplate> addedTemplates = reader.addAdditionalTemplates(config.getCloud(), listFiles);
        logger.info("initCloudTemplates - Added the following templates: " + addedTemplates);
        config.getLastTemplateFileNum().addAndGet(listFiles.length);

    }

    private CloudConfigurationHolder getCloudConfigurationFromManagementSpace() {
        logger.info("Waiting for cloud configuration to become available in management space");
        final CloudConfigurationHolder config = gigaSpace.read(
                new CloudConfigurationHolder(), 1000 * 60);
        if (config == null) {

            final String msg = "Could not find the expected Cloud Configuration Holder in Management space!";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        return config;
    }

    @Override
    public Class<?> getObjectType() {
        return RestConfiguration.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
