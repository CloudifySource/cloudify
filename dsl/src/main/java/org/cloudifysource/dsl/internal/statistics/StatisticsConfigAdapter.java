
package org.cloudifysource.dsl.internal.statistics;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.cloudifysource.dsl.statistics.InstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.TimeWindowStatisticsConfig;

/**
 * Create an openspaces configuration using the the configuration POJO defined in the DSL POJO.
 * 
 * @author adaml
 *
 */
public class StatisticsConfigAdapter {

	private static final String OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME = "org.openspaces.admin.pu.statistics";

	/**
	 * Create an openspaces statistics object using the DSL config POJO.
	 * @param config
	 * 			instance statistics config DSL POJO.
	 * @return
	 * 			the equivalent openspaces statistics config object.
	 * @throws InstantiationException .
	 * @throws IllegalAccessException .
	 * @throws ClassNotFoundException .
	 * @throws InvocationTargetException .
	 */
	public org.openspaces.admin.pu.statistics.InstancesStatisticsConfig createInstanceStatistics(
			final InstancesStatisticsConfig config) 
					throws InstantiationException, IllegalAccessException, ClassNotFoundException, 
					InvocationTargetException {
		final String configClassName = config.getClass().getSimpleName();
		final String openspacesInstanceStatisticsClass = OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME 
				+ "." +  configClassName;
		Object obj = Class.forName(openspacesInstanceStatisticsClass).newInstance();
		System.out.println(obj.getClass().getSimpleName());
		//copy all properties from DSL POJO to its equivalent openspaces object. 
		BeanUtils.copyProperties(obj, config);
		return (org.openspaces.admin.pu.statistics.InstancesStatisticsConfig) obj;
	}

	/**
	 * Create an openspaces time window statistics object using the DSL config POJO.
	 * @param config
	 * 			instance statistics config DSL POJO.
	 * @return
	 * 		the equivalent openspaces statistics config object.
	 * @throws InstantiationException .
	 * @throws IllegalAccessException . 
	 * @throws ClassNotFoundException .
	 * @throws InvocationTargetException .
	 */
	public org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig createTimeWindowStatistics(
			final TimeWindowStatisticsConfig config) 
					throws InstantiationException, IllegalAccessException, ClassNotFoundException, 
					InvocationTargetException {
		final String configClassName = config.getClass().getSimpleName();
		final String openspacesInstanceStatisticsClass = OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME 
				+ "." +  configClassName;
		Object obj = Class.forName(openspacesInstanceStatisticsClass).newInstance();
		System.out.println(obj.getClass().getSimpleName());
		//copy all properties from DSL POJO to its equivalent openspaces object. 
		BeanUtils.copyProperties(obj, config);
		return (org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig) obj;
	}

}
