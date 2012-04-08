package org.cloudifysource.rest.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.DataGrid;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.cloudifysource.dsl.statistics.AbstractStatisticsDetails;
import org.cloudifysource.dsl.statistics.ServiceStatisticsDetails;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleRuleConfig;
import org.openspaces.admin.pu.elastic.config.CapacityRequirementsConfig;
import org.openspaces.admin.pu.elastic.config.CapacityRequirementsConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.statistics.LastSampleTimeWindowStatisticsConfig;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsId;
import org.openspaces.core.util.MemoryUnit;

public class ElasticScaleConfigFactory {

	private static final Logger logger = Logger.getLogger(ElasticScaleConfigFactory.class.getName());


	/**
	 * @param serviceName - the absolute name of the service
	 * @param service - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB - MB memory allocated for the GSC plus the external service.
	 * @return a @{link ManualCapacityScaleConfig} based on the specified service and memory.
	 */
	public static ManualCapacityScaleConfig createManualCapacityScaleConfig(
			final String serviceName, 
			Service service,
			final int externalProcessMemoryInMB)
			throws DSLException {

		int numberOfInstances = 1;
		if (service == null) {
			logger.info("Deploying service " + serviceName + " without a recipe. Assuming number of instances is 1");
		} else if (service.getNumInstances() > 0) {
			numberOfInstances = service.getNumInstances();
			logger.info("Deploying service " + serviceName + " with " + numberOfInstances + " instances.");
		} else {
			throw new DSLException("Number of instances must be at least 1");
		}

		return new ManualCapacityScaleConfigurer().memoryCapacity(
				externalProcessMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES).create();
	}

	public static EagerScaleConfig createEagerScaleConfig() {
		return new EagerScaleConfigurer().atMostOneContainerPerMachine().create();
	}

	public static ManualCapacityScaleConfig createManualCapacityScaleConfig(final DataGrid dataGridConfig) {
		return new ManualCapacityScaleConfigurer().memoryCapacity(
				dataGridConfig.getSla().getMemoryCapacity(), MemoryUnit.MEGABYTES).create();
	}

	/**
	 * @param serviceName - the absolute name of the service
	 * @param service - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB - MB memory allocated for the GSC plus the external service.
	 * @return a @{link AutomaticCapacityScaleConfig} based on the specified service and memory.
	 */
	public static AutomaticCapacityScaleConfig createAutomaticCapacityScaleConfig(
			final String serviceName,
			final Service service, 
			final int externalProcessMemoryInMB)
			throws DSLException {

		List<ScalingRuleDetails> scalingRules = service.getScalingRules();
		if (scalingRules.size() == 0) {
			throw new DSLException("scalingRules cannot be empty");
		}
		
		if (service.getMinAllowedInstances() <= 0) {
			throw new DSLException("Minimum number of instances (" + service.getMinAllowedInstances()
					+ ") must be 1 or higher.");
		}
		
		if (service.getMinAllowedInstances() > service.getMaxAllowedInstances()) {
			throw new DSLException("maximum number of instances (" + service.getMaxAllowedInstances()
					+ ") must be equal or greater than the minimum number of instances ("
					+ service.getMinAllowedInstances() + ")");
		}
		
		if (service.getMinAllowedInstances() > service.getNumInstances()) {
			throw new DSLException("number of instances (" + service.getNumInstances()
					+ ") must be equal or greater than the minimum number of instances ("
					+ service.getMinAllowedInstances() + ")");
		}
		
		if (service.getNumInstances() > service.getMaxAllowedInstances()) {
			throw new DSLException("number of instances (" + service.getNumInstances()
					+ ") must be equal or less than the maximum number of instances ("
					+ service.getMaxAllowedInstances() + ")");
		}
		
		CapacityRequirementsConfig minCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((service.getMinAllowedInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();
		
		CapacityRequirementsConfig initialCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((service.getNumInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();
	
		
		CapacityRequirementsConfig maxCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((service.getMaxAllowedInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();
	
		AutomaticCapacityScaleConfigurer scaleConfigurer = 
			new AutomaticCapacityScaleConfigurer()
			.minCapacity(minCapacity)
			.initialCapacity(initialCapacity)
			.maxCapacity(maxCapacity)
			.statisticsPollingInterval(service.getSamplingPeriodInSeconds(), TimeUnit.SECONDS)
			.cooldownAfterScaleOut(service.getScaleOutCooldownInSeconds(),TimeUnit.SECONDS)
			.cooldownAfterScaleIn(service.getScaleInCooldownInSeconds(),TimeUnit.SECONDS);
			
		Map<String, ServiceStatisticsDetails> serviceStatisticsByName = new HashMap<String, ServiceStatisticsDetails>();
		for (AbstractStatisticsDetails calculatedStatistics : service.getServiceStatistics()) {
			if (calculatedStatistics instanceof ServiceStatisticsDetails) {
				serviceStatisticsByName.put(calculatedStatistics.getName(), (ServiceStatisticsDetails)calculatedStatistics);
			}
		}
		
		if (serviceStatisticsByName.isEmpty()) {
			throw new DSLException("calculatedStatistics must define at least one serviceStatistics entry");
		}
		
		for (ScalingRuleDetails scalingRule : scalingRules) {
			
			String serviceStatisticsName = scalingRule.getStatistics();
			if (serviceStatisticsName == null) {
				throw new DSLException("scalingRule must specify statistics (serviceStatistics name)");
			}
			
			ServiceStatisticsDetails serviceStatistics = serviceStatisticsByName.get(serviceStatisticsName);
			
			if (serviceStatistics == null) {
				throw new DSLException("scalingRule must specify a valid statistics (serviceStatistics name). " + serviceStatisticsName + " is not recognized. Possible values are: "+ serviceStatisticsByName.keySet());
			}
		
			ProcessingUnitStatisticsId statisticsId = new ProcessingUnitStatisticsId();
			statisticsId.setMonitor(CloudifyConstants.USM_MONITORS_SERVICE_ID);
			statisticsId.setMetric(serviceStatistics.getMetric());
			statisticsId.setInstancesStatistics(serviceStatistics.getInstancesStatistics().createInstancesStatistics());
	
			if (serviceStatistics.getMovingTimeRangeInSeconds() <= service.getSamplingPeriodInSeconds()) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Deploying service " + serviceName + " with auto scaling that monitors the last sample of "
							+ serviceStatistics.getMetric());
				}
				statisticsId.setTimeWindowStatistics(new LastSampleTimeWindowStatisticsConfig());
			} else {
				statisticsId.setTimeWindowStatistics(serviceStatistics.getTimeStatistics().createTimeWindowStatistics(
						serviceStatistics.getMovingTimeRangeInSeconds(), TimeUnit.SECONDS));
			}
	
			AutomaticCapacityScaleRuleConfig rule = new AutomaticCapacityScaleRuleConfig();
			rule.setStatistics(statisticsId);
			
			if (scalingRule.getLowThreshold() == null){
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(serviceName + " scalingRule for " + serviceStatisticsName +" lowThreshold is undefined");
				}
			}
			else {
				
				Comparable<?> threshold = scalingRule.getLowThreshold().getValue();
				if (threshold == null) {
					throw new DSLException(serviceName + " scalingRule for " + serviceStatisticsName +" lowThreshold value is missing");
				}
				
				int instancesDecrease = scalingRule.getLowThreshold().getInstancesDecrease ();
				if (instancesDecrease < 0) {
					throw new DSLException(serviceName + " scalingRule for " + serviceStatisticsName +" lowThreshold instancesDecrease cannot be a negative number ("+instancesDecrease+")");
				}
				
				if (instancesDecrease == 0) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(serviceName + " scalingRule for " + serviceStatisticsName +" lowThreshold instancesDecrease is 0");
					}
				}
				else {
					rule.setLowThreshold(threshold);
					rule.setLowThresholdBreachedDecrease(
							new CapacityRequirementsConfigurer()
							.memoryCapacity(instancesDecrease * externalProcessMemoryInMB, MemoryUnit.MEGABYTES)
							.create());
				}
			}
			
			if (scalingRule.getHighThreshold() == null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(serviceName + " scalingRule for " + serviceStatisticsName +" highThreshold is undefined");
				}
			}
			else {
				Comparable<?> threshold = scalingRule.getHighThreshold().getValue();
				if (threshold == null) {
					throw new DSLException(serviceName + " scalingRule for " + serviceStatisticsName +" highThreshold value is missing");
				}
				
				int instancesIncrease = scalingRule.getHighThreshold().getInstancesIncrease();
				if (instancesIncrease < 0) {
					throw new DSLException(serviceName + " scalingRule for " + serviceStatisticsName +" highThreshold instancesIncrease cannot be a negative number ("+instancesIncrease+")");
				}
				
				if (instancesIncrease == 0) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(serviceName + " scalingRule for " + serviceStatisticsName +" highThreshold instancesIncrease is 0");
					}
				}
				else {
					rule.setHighThreshold(threshold);
					rule.setHighThresholdBreachedIncrease(
							new CapacityRequirementsConfigurer()
							.memoryCapacity(instancesIncrease * externalProcessMemoryInMB, MemoryUnit.MEGABYTES)
							.create());
				}
			}
			
			scaleConfigurer.addRule(rule);
		}
		
		return scaleConfigurer.create();
	}

}
