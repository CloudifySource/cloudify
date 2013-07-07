package org.cloudifysource.dsl;

import java.lang.reflect.InvocationTargetException;

import org.cloudifysource.dsl.internal.statistics.OpenspacesDomainAdapter;
import org.cloudifysource.dsl.statistics.AverageInstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.AverageTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.CpuPercentageTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.EachSingleInstanceStatisticsConfig;
import org.cloudifysource.dsl.statistics.MaximumInstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.MaximumTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.MinimumInstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.MinimumTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.PercentileInstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.PercentileTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.ThroughputTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.TimeWindowStatisticsConfig;
import org.junit.Assert;
import org.junit.Test;
import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;

/**
 * 
 * @author adaml
 *
 */
public class OpenspacesDomainAdapterTest {

	private OpenspacesDomainAdapter configFactory = new OpenspacesDomainAdapter();
	
	@Test
	public void testPercentileInstancesStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		PercentileInstancesStatisticsConfig config = new PercentileInstancesStatisticsConfig();
		config.setPercentile(10);
		InstancesStatisticsConfig instanceStatistics = (InstancesStatisticsConfig)configFactory.createStatisticsConfig(config);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof PercentileInstancesStatisticsConfig",
				instanceStatistics instanceof org.openspaces.admin.pu.statistics.PercentileInstancesStatisticsConfig);
		org.openspaces.admin.pu.statistics.PercentileInstancesStatisticsConfig percentileConfig =
				(org.openspaces.admin.pu.statistics.PercentileInstancesStatisticsConfig) instanceStatistics;
		Assert.assertTrue("Expecting percentile to be set to 10", percentileConfig.getPercentile() == 10);
	}
	
	@Test
	public void testAverageInstancesStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		AverageInstancesStatisticsConfig config = new AverageInstancesStatisticsConfig();
		InstancesStatisticsConfig instanceStatistics = (InstancesStatisticsConfig)configFactory.createStatisticsConfig(config);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof AverageInstancesStatisticsConfig",
				instanceStatistics instanceof org.openspaces.admin.pu.statistics.AverageInstancesStatisticsConfig);
	}
	
	@Test
	public void testEachSingleInstanceStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		EachSingleInstanceStatisticsConfig config = new EachSingleInstanceStatisticsConfig();
		InstancesStatisticsConfig instanceStatistics = (InstancesStatisticsConfig)configFactory.createStatisticsConfig(config);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof EachSingleInstanceStatisticsConfig",
				instanceStatistics instanceof org.openspaces.admin.pu.statistics.EachSingleInstanceStatisticsConfig);
	}
	
	@Test
	public void testMaximumInstancesStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		MaximumInstancesStatisticsConfig config = new MaximumInstancesStatisticsConfig();
		InstancesStatisticsConfig instanceStatistics = (InstancesStatisticsConfig)configFactory.createStatisticsConfig(config);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof MaximumInstancesStatisticsConfig",
				instanceStatistics instanceof org.openspaces.admin.pu.statistics.MaximumInstancesStatisticsConfig);
	}
	
	@Test
	public void testMinimumInstancesStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		MinimumInstancesStatisticsConfig config = new MinimumInstancesStatisticsConfig();
		InstancesStatisticsConfig instanceStatistics = (InstancesStatisticsConfig)configFactory.createStatisticsConfig(config);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof MinimumInstancesStatisticsConfig",
				instanceStatistics instanceof org.openspaces.admin.pu.statistics.MinimumInstancesStatisticsConfig);
	}
	
	@Test
	public void testAverageTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new AverageTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof AverageTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.AverageTimeWindowStatisticsConfig);
	}

	void assertDefaultTimeWindowValues(
			org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics) {
		Assert.assertTrue("Expected default value not set.", 
				((org.openspaces.admin.pu.statistics.AbstractTimeWindowStatisticsConfig)timeWindowStatistics).getTimeWindowSeconds() == 60);
		Assert.assertTrue("Expected default value not set.", 
				((org.openspaces.admin.pu.statistics.AbstractTimeWindowStatisticsConfig)timeWindowStatistics).getMinimumTimeWindowSeconds() == 60);
		Assert.assertTrue("Expected default value not set.", 
				((org.openspaces.admin.pu.statistics.AbstractTimeWindowStatisticsConfig)timeWindowStatistics).getMaximumTimeWindowSeconds() == 120);
	}
	
	@Test
	public void testCpuPercentageTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new CpuPercentageTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof CpuPercentageTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.CpuPercentageTimeWindowStatisticsConfig);
	}
	
	@Test
	public void testMaximumTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new MaximumTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof MaximumTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.MaximumTimeWindowStatisticsConfig);
	}
	
	@Test
	public void testMinimumTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new MinimumTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof MinimumTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.MinimumTimeWindowStatisticsConfig);
	}
	
	@Test
	public void testPercentileTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new PercentileTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof PercentileTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.PercentileTimeWindowStatisticsConfig);
	}
	
	@Test
	public void testThroughputTimeWindowStatisticsConfig() 
			throws InstantiationException, IllegalAccessException, 
			ClassNotFoundException, InvocationTargetException {
		TimeWindowStatisticsConfig config = new ThroughputTimeWindowStatisticsConfig();
		org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig timeWindowStatistics = 
				(org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig)configFactory.createStatisticsConfig(config);
		
		assertDefaultTimeWindowValues(timeWindowStatistics);
		Assert.assertTrue("InstanceStatistics is expected to be instanceof ThroughputTimeWindowStatisticsConfig",
				timeWindowStatistics instanceof org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfig);
	}
	
}
