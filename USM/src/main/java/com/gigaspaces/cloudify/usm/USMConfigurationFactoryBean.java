package com.gigaspaces.cloudify.usm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.properties.BeanLevelProperties;
import org.openspaces.core.properties.BeanLevelPropertiesAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.DSLServiceCompilationResult;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.usm.dsl.DSLConfiguration;

@Component
public class USMConfigurationFactoryBean implements FactoryBean<UniversalServiceManagerConfiguration>,
		ApplicationContextAware, ClusterInfoAware, BeanLevelPropertiesAware {

	

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(USMConfigurationFactoryBean.class.getName());

	// Classes used for unmarshal XML file
	// private List<String> contextClasses = new ArrayList<String>();

	private File puWorkDir;

	private File puExtDir;

	private String usmConfigurationFileName = "usm.xml";

	private ClusterInfo clusterInfo;

	private String serviceFileName = null;

	private String propertiesFileName;

	private boolean isRunningInGSC;


	private String applicationName;

	@Override
	public UniversalServiceManagerConfiguration getObject() throws USMException {

		try {
			return handleDsl();
		} catch (FileNotFoundException e) {
			throw new USMException(e);
		} catch (PackagingException e) {
			throw new USMException(e);
		}
	}

	private UniversalServiceManagerConfiguration handleDsl() throws USMException, FileNotFoundException, PackagingException {
		File dslFile;
		if (serviceFileName == null) {
			dslFile = ServiceReader.findServiceFile(puExtDir);
		}
		else {
			dslFile = new File(puExtDir,serviceFileName);
			if (!dslFile.isFile()) {
				throw new FileNotFoundException("Cannot find file " + dslFile.getAbsolutePath());
			}
		}
		
		logger.info("Found service configuration file: " + dslFile.getAbsolutePath());

		DSLServiceCompilationResult compilationResult;
		try {
			// TODO - ADD THE ADMIN HERE!!!
			if (this.clusterInfo == null) {
				throw new IllegalStateException(
						"The cluster information is missing. " +
								"If running in the integrated container, use the -cluster option to set the cluster info manually");
			}
			
			compilationResult = ServiceReader.getServiceFromFile(dslFile, this.puExtDir, USMUtils.getAdmin(), 
					this.clusterInfo, this.propertiesFileName, this.isRunningInGSC);
			
		} catch (final Exception e) {
			throw new USMException("Failed to read service from file: " + dslFile, e);
		}

		final DSLConfiguration config = new DSLConfiguration(compilationResult.getService(), compilationResult.getContext(),  this.puExtDir, dslFile);

		return config;

	}

	@Override
	public Class<?> getObjectType() {
		return UniversalServiceManagerConfiguration.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setApplicationContext(final ApplicationContext ctx) throws BeansException {

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
			this.applicationName = props.getProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME);
		}
	}

}
