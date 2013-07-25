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
package org.cloudifysource.usm;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.context.ServiceContextImpl;
import org.cloudifysource.usm.dsl.ServiceConfiguration;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.properties.BeanLevelProperties;
import org.openspaces.core.properties.BeanLevelPropertiesAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/***************************
 * Configuration factory for the USM component. Returns a configuration bean. Note that this was useful when we had
 * multiple input types for services. Now that we have standardized on groovy for all services, this is kind of
 * redundant.
 *
 * @author barakme
 * @since 1.0
 *
 */
@Component
public class USMConfigurationFactoryBean implements FactoryBean<ServiceConfiguration>,
		ApplicationContextAware, ClusterInfoAware, BeanLevelPropertiesAware {

	private File puWorkDir;

	private File puExtDir;

	private String usmConfigurationFileName = "usm.xml";

	private ClusterInfo clusterInfo;

	private String serviceFileName = null;

	private String propertiesFileName;

	private boolean isRunningInGSC;

	private static final Logger logger =
			Logger.getLogger(USMConfigurationFactoryBean.class.getName());

	@Override
	public ServiceConfiguration getObject() throws USMException {

		try {
			ServiceConfiguration handleDsl = handleDsl();
			logger.info("Successfully read Groovy based DSL");
			return handleDsl;
		} catch (DSLException e) {
			throw new USMException(e);
		} catch (IOException e) {
			throw new USMException(e);
		}
	}

	private ServiceConfiguration handleDsl() throws DSLException, IOException {
		File dslFile = null;

		if (serviceFileName != null) {
			dslFile = new File(this.puExtDir, this.serviceFileName);
		}
		ServiceContext serviceContext = new ServiceContextImpl(clusterInfo, this.puExtDir.getCanonicalPath());
		ClusterInfo clusterInfoToUseInGsc = this.clusterInfo;
		if (clusterInfoToUseInGsc == null) {
			clusterInfoToUseInGsc = new ClusterInfo(null, 1, 0, 1, 0);
		}

		// create an uninitialized service context
		String canonicalPath = null;
		try {
			canonicalPath = this.puExtDir.getCanonicalPath();
		} catch (IOException e) {
			throw new DSLException("Failed to get canonical path of work directory: " + canonicalPath + ". Error was: "
					+ e.getMessage(), e);
		}
		//TODO: Why are these the same
		if (isRunningInGSC) {
			serviceContext = new ServiceContextImpl(clusterInfoToUseInGsc, canonicalPath);
		} else {
			serviceContext = new ServiceContextImpl(new ClusterInfo(null, 1, 0, 1, 0), canonicalPath);
		}

		DSLReader dslReader = new DSLReader();
		dslReader.setContext(serviceContext);
		dslReader.setPropertiesFileName(propertiesFileName);
		dslReader.setRunningInGSC(isRunningInGSC);
		dslReader.setDslFile(dslFile);
		dslReader.setWorkDir(this.puExtDir);
		dslReader.setDslFileNameSuffix(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);

		// When loading a service in the USM, expect the jar files to
		// be available in the pu lib dir, and ignore the contents of usmlib
		dslReader.setLoadUsmLib(false);

		logger.info("Loading Service configuration from DSL File");
		Service service = dslReader.readDslEntity(Service.class);
		if (isRunningInGSC) {
			if (clusterInfoToUseInGsc.getName() == null) {
				clusterInfoToUseInGsc.setName(ServiceUtils.getAbsolutePUName(
						CloudifyConstants.DEFAULT_APPLICATION_NAME, service.getName()));
			}
			
			((ServiceContextImpl)serviceContext).init(service, USMUtils.getAdmin(), clusterInfoToUseInGsc);
		} else {
			((ServiceContextImpl)serviceContext).initInIntegratedContainer(service);
		}
		
		return new ServiceConfiguration(service, serviceContext, this.puExtDir, dslReader.getDslFile(),
				dslReader.getDSLClassLoader());
	}

	@Override
	public Class<?> getObjectType() {
		return ServiceConfiguration.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setApplicationContext(final ApplicationContext ctx) {

		this.isRunningInGSC = USMUtils.isRunningInGSC(ctx);
		this.puWorkDir = USMUtils.getPUWorkDir(ctx);
		this.puExtDir = new File(puWorkDir, "ext");

	}

	public String getUsmConfigurationFileName() {
		return usmConfigurationFileName;
	}

	public void setUsmConfigurationFileName(final String usmConfigurationFileName) {
		this.usmConfigurationFileName = usmConfigurationFileName;
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;

	}

	@Override
	public void setBeanLevelProperties(final BeanLevelProperties beanLevelProperties) {
		final Properties props = beanLevelProperties.getContextProperties();
		if (props != null) {
			this.serviceFileName = props.getProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME);
			this.propertiesFileName = props.getProperty(CloudifyConstants.CONTEXT_PROPERTY_PROPERTIES_FILE_NAME);

		}
	}

}
