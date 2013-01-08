package org.cloudifysource.rest.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.cloudifysource.dsl.statistics.AbstractStatisticsDetails;
import org.cloudifysource.dsl.statistics.ServiceStatisticsDetails;
import org.cloudifysource.rest.util.IsolationUtils;
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
import org.openspaces.admin.zone.config.AnyZonesConfig;
import org.openspaces.core.util.MemoryUnit;

/**
 * 
 * @author elip
 *
 */
public final class ElasticScaleConfigFactory {
	
	private ElasticScaleConfigFactory() {
		
	}

	private static final String SCALING_RULE_MUST_SPECIFY_SERVICE_STATISTICS_ERROR_MSG = 
			"scalingRule must specify serviceStatistics (either a closure or " 
					+ "reference a predefined serviceStatistics name).";
	private static final Logger logger = Logger
			.getLogger(ElasticScaleConfigFactory.class.getName());

	/**
	 * 
	 * @param totalMemoryInMB .
	 * @param totalCpuCores . 
	 * @param locationAware .
	 * @param dedicated .
	 * @return .
	 */
	public static ManualCapacityScaleConfig createManualCapacityScaleConfig(
			final int totalMemoryInMB, final double totalCpuCores,
			final boolean locationAware, final boolean dedicated) {
		final ManualCapacityScaleConfig config = new ManualCapacityScaleConfigurer()
				.memoryCapacity(totalMemoryInMB, MemoryUnit.MEGABYTES)
				.numberOfCpuCores(totalCpuCores).create();
		config.setGridServiceAgentZonesAware(locationAware);
		if (dedicated) {
			config.setAtMostOneContainerPerMachine(true);
		}
		return config;
	}

	/**
	 * 
	 * @return .
	 */
	public static EagerScaleConfig createEagerScaleConfig() {
		return new EagerScaleConfigurer().atMostOneContainerPerMachine()
				.create();
	}

	/**
	 * @param serviceName
	 *            - the absolute name of the service
	 * @param service
	 *            - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB
	 *            - MB memory allocated for the GSC plus the external service.
	 * @param locationAware .
	 * @return a @{link AutomaticCapacityScaleConfig} based on the specified
	 *         service and memory.
	 * @throws DSLException .
	 */
	public static AutomaticCapacityScaleConfig createAutomaticCapacityScaleConfig(
			final String serviceName, final Service service,
			final int externalProcessMemoryInMB, 
			final boolean locationAware, final boolean dedicated)
			throws DSLException {

		if (externalProcessMemoryInMB <= 0) {
			throw new IllegalArgumentException(
					"externalProcessMemoryInMB must be positive");
		}

		final List<ScalingRuleDetails> scalingRules = service.getScalingRules();
		if (scalingRules.isEmpty()) {
			throw new DSLException("scalingRules cannot be empty");
		}

		if (service.getMinAllowedInstances() <= 0) {
			throw new DSLException("Minimum number of instances ("
					+ service.getMinAllowedInstances()
					+ ") must be 1 or higher.");
		}

		if (service.getMinAllowedInstances() > service.getMaxAllowedInstances()) {
			throw new DSLException(
					"maximum number of instances ("
							+ service.getMaxAllowedInstances()
							+ ") must be equal or greater than the minimum number of instances ("
							+ service.getMinAllowedInstances() + ")");
		}

		if (service.getMinAllowedInstances() > service.getNumInstances()) {
			throw new DSLException(
					"number of instances ("
							+ service.getNumInstances()
							+ ") must be equal or greater than the minimum number of instances ("
							+ service.getMinAllowedInstances() + ")");
		}

		if (service.getNumInstances() > service.getMaxAllowedInstances()) {
			throw new DSLException(
					"number of instances ("
							+ service.getNumInstances()
							+ ") must be equal or less than the maximum number of instances ("
							+ service.getMaxAllowedInstances() + ")");
		}

		double instanceCpuCores = IsolationUtils.getInstanceCpuCores(service);
		final CapacityRequirementsConfig minCapacity = new CapacityRequirementsConfigurer()
				.memoryCapacity(
						service.getMinAllowedInstances()
								* externalProcessMemoryInMB,
						MemoryUnit.MEGABYTES)
				.numberOfCpuCores(
						service.getMinAllowedInstances() * instanceCpuCores)
				.create();

		final CapacityRequirementsConfig initialCapacity = new CapacityRequirementsConfigurer()
				.memoryCapacity(
						service.getNumInstances() * externalProcessMemoryInMB,
						MemoryUnit.MEGABYTES)
				.numberOfCpuCores(service.getNumInstances() * instanceCpuCores)
				.create();

		final CapacityRequirementsConfig maxCapacity = new CapacityRequirementsConfigurer()
				.memoryCapacity(
						service.getMaxAllowedInstances()
								* externalProcessMemoryInMB,
						MemoryUnit.MEGABYTES)
				.numberOfCpuCores(
						service.getMaxAllowedInstances() * instanceCpuCores)
				.create();

		final CapacityRequirementsConfig minCapacityPerZone = new CapacityRequirementsConfigurer()
				.memoryCapacity(
						service.getMinAllowedInstancesPerLocation()
								* externalProcessMemoryInMB,
						MemoryUnit.MEGABYTES)
				.numberOfCpuCores(
						service.getMinAllowedInstancesPerLocation()
								* instanceCpuCores).create();

		final CapacityRequirementsConfig maxCapacityPerZone = new CapacityRequirementsConfigurer()
				.memoryCapacity(
						service.getMaxAllowedInstancesPerLocation()
								* externalProcessMemoryInMB,
						MemoryUnit.MEGABYTES)
				.numberOfCpuCores(
						service.getMaxAllowedInstancesPerLocation()
								* instanceCpuCores).create();

		final AutomaticCapacityScaleConfigurer scaleConfigurer = new AutomaticCapacityScaleConfigurer()
				.minCapacity(minCapacity)
				.initialCapacity(initialCapacity)
				.maxCapacity(maxCapacity)
				.minCapacityPerZone(minCapacityPerZone)
				.maxCapacityPerZone(maxCapacityPerZone)
				.statisticsPollingInterval(
						service.getSamplingPeriodInSeconds(), TimeUnit.SECONDS)
				.cooldownAfterScaleOut(service.getScaleOutCooldownInSeconds(),
						TimeUnit.SECONDS)
				.cooldownAfterScaleIn(service.getScaleInCooldownInSeconds(),
						TimeUnit.SECONDS);

		if (locationAware) {
			scaleConfigurer.enableGridServiceAgentZonesAware();
		}
		if (dedicated) {
			scaleConfigurer.atMostOneContainerPerMachine();
		}

		final Map<String, ServiceStatisticsDetails> 
			serviceStatisticsByName = new HashMap<String, ServiceStatisticsDetails>();
		if (service.getServiceStatistics() != null) {
			for (final AbstractStatisticsDetails calculatedStatistics : service
					.getServiceStatistics()) {
				if (calculatedStatistics instanceof ServiceStatisticsDetails) {
					serviceStatisticsByName.put(calculatedStatistics.getName(),
							(ServiceStatisticsDetails) calculatedStatistics);
				}
			}
		}

		for (final ScalingRuleDetails scalingRule : scalingRules) {

			final Object serviceStatisticsObject = scalingRule
					.getServiceStatistics();
			if (serviceStatisticsObject == null) {
				throw new DSLException(
						SCALING_RULE_MUST_SPECIFY_SERVICE_STATISTICS_ERROR_MSG);
			}

			ServiceStatisticsDetails serviceStatistics = null;

			if (serviceStatisticsObject instanceof String) {
				final String serviceStatisticsName = (String) serviceStatisticsObject;
				serviceStatistics = serviceStatisticsByName
						.get(serviceStatisticsName);

				if (serviceStatistics == null) {
					throw new DSLException(
							SCALING_RULE_MUST_SPECIFY_SERVICE_STATISTICS_ERROR_MSG
									+ " "
									+ serviceStatisticsName
									+ " is not recognized. Possible values are: "
									+ serviceStatisticsByName.keySet());
				}
			} else if (serviceStatisticsObject instanceof ServiceStatisticsDetails) {
				serviceStatistics = (ServiceStatisticsDetails) serviceStatisticsObject;
			} else {
				throw new DSLException(
						SCALING_RULE_MUST_SPECIFY_SERVICE_STATISTICS_ERROR_MSG
								+ " Unsupported type "
								+ serviceStatisticsObject.getClass());
			}

			final ProcessingUnitStatisticsId statisticsId = new ProcessingUnitStatisticsId();
			statisticsId.setMonitor(CloudifyConstants.USM_MONITORS_SERVICE_ID);
			statisticsId.setMetric(serviceStatistics.getMetric());
			statisticsId.setInstancesStatistics(serviceStatistics
					.getInstancesStatistics().createInstancesStatistics());

			if (serviceStatistics.getMovingTimeRangeInSeconds() <= service
					.getSamplingPeriodInSeconds()) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Deploying service "
							+ serviceName
							+ " with auto scaling that monitors the last sample of "
							+ serviceStatistics.getMetric());
				}
				statisticsId
						.setTimeWindowStatistics(new LastSampleTimeWindowStatisticsConfig());
			} else {
				statisticsId
						.setTimeWindowStatistics(serviceStatistics
								.getTimeStatistics()
								.createTimeWindowStatistics(
										serviceStatistics
												.getMovingTimeRangeInSeconds(),
										TimeUnit.SECONDS));
			}

			statisticsId.setAgentZones(new AnyZonesConfig());

			final AutomaticCapacityScaleRuleConfig rule = new AutomaticCapacityScaleRuleConfig();
			rule.setStatistics(statisticsId);

			if (scalingRule.getLowThreshold() == null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(serviceName + " scalingRule for "
							+ serviceStatistics.getMetric()
							+ " lowThreshold is undefined");
				}
			} else {

				final Comparable<?> threshold = scalingRule.getLowThreshold()
						.getValue();
				if (threshold == null) {
					throw new DSLException(serviceName + " scalingRule for "
							+ serviceStatistics.getMetric()
							+ " lowThreshold value is missing");
				}

				final int instancesDecrease = scalingRule.getLowThreshold()
						.getInstancesDecrease();
				if (instancesDecrease < 0) {
					throw new DSLException(
							serviceName
									+ " scalingRule for "
									+ serviceStatistics.getMetric()
									+ " lowThreshold instancesDecrease cannot be a negative number ("
									+ instancesDecrease + ")");
				}

				if (instancesDecrease == 0) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(serviceName + " scalingRule for "
								+ serviceStatistics.getMetric()
								+ " lowThreshold instancesDecrease is 0");
					}
				} else {
					rule.setLowThreshold(threshold);
					rule.setLowThresholdBreachedDecrease(new CapacityRequirementsConfigurer()
							.memoryCapacity(
									instancesDecrease
											* externalProcessMemoryInMB,
									MemoryUnit.MEGABYTES).create());
				}
			}

			if (scalingRule.getHighThreshold() == null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(serviceName + " scalingRule for "
							+ serviceStatistics.getMetric()
							+ " highThreshold is undefined");
				}
			} else {
				final Comparable<?> threshold = scalingRule.getHighThreshold()
						.getValue();
				if (threshold == null) {
					throw new DSLException(serviceName + " scalingRule for "
							+ serviceStatistics.getMetric()
							+ " highThreshold value is missing");
				}

				final int instancesIncrease = scalingRule.getHighThreshold()
						.getInstancesIncrease();
				if (instancesIncrease < 0) {
					throw new DSLException(
							serviceName
									+ " scalingRule for "
									+ serviceStatistics.getMetric()
									+ " highThreshold instancesIncrease cannot be a negative number ("
									+ instancesIncrease + ")");
				}

				if (instancesIncrease == 0) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(serviceName + " scalingRule for "
								+ serviceStatistics.getMetric()
								+ " highThreshold instancesIncrease is 0");
					}
				} else {
					rule.setHighThreshold(threshold);
					rule.setHighThresholdBreachedIncrease(new CapacityRequirementsConfigurer()
							.memoryCapacity(
									instancesIncrease
											* externalProcessMemoryInMB,
									MemoryUnit.MEGABYTES).create());
				}
			}

			scaleConfigurer.addRule(rule);
		}

		return scaleConfigurer.create();
	}

}
