package org.cloudifysource.rest.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.cloudifysource.domain.statistics.AbstractConfig;
import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

/**
 * Converts Domain POJO objects into openspaces config POJOS.
 * 
 * @author adaml
 *
 */
public class OpenspacesDomainStatisticsAdapter {
	private static final String OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME = "org.openspaces.admin.pu.statistics";

	private org.openspaces.admin.config.AbstractConfig createStatisticsConfig(
			final AbstractConfig config) throws InstantiationException, 
			IllegalAccessException, ClassNotFoundException, InvocationTargetException {
		final String configClassName = config.getClass().getSimpleName();
		final String openspacesInstanceStatisticsClass = OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME 
				+ "." +  configClassName;
		Object obj = Class.forName(openspacesInstanceStatisticsClass).newInstance();
		//copy all properties from DSL POJO to its equivalent openspaces object. 
		BeanUtils.copyProperties(obj, config);
		return (org.openspaces.admin.config.AbstractConfig) obj;
	}
	
	/**
	 * Create an openspaces instance statistics config object using the DSL config POJO.
	 * @param config
	 * 			instance statistics config DSL POJO.
	 * @return
	 * 			the equivalent openspaces instance statistics config object.
	 * @throws InstantiationException .
	 * @throws IllegalAccessException .
	 * @throws ClassNotFoundException .
	 * @throws InvocationTargetException .
	 */
	public InstancesStatisticsConfig createInstanceStatisticsConfig(final AbstractConfig config) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
		return (InstancesStatisticsConfig) createStatisticsConfig(config);
	}
	
	/**
	 * Create an openspaces time window statistics config object using the DSL config POJO.
	 * @param config
	 * 			instance statistics config DSL POJO.
	 * @return
	 * 			the equivalent openspaces time window statistics config object.
	 * @throws InstantiationException .
	 * @throws IllegalAccessException .
	 * @throws ClassNotFoundException .
	 * @throws InvocationTargetException .
	 */
	public TimeWindowStatisticsConfig createTimeWindowStatisticsConfig(final AbstractConfig config) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
		return (TimeWindowStatisticsConfig) createStatisticsConfig(config);
	}
}
