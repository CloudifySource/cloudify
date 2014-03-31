package org.cloudifysource.dsl;

import junit.framework.Assert;
import org.cloudifysource.domain.cloud.AgentComponent;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.DeployerComponent;
import org.cloudifysource.domain.cloud.DiscoveryComponent;
import org.cloudifysource.domain.cloud.GridComponents;
import org.cloudifysource.domain.cloud.OrchestratorComponent;
import org.cloudifysource.domain.cloud.RestComponent;
import org.cloudifysource.domain.cloud.UsmComponent;
import org.cloudifysource.domain.cloud.WebuiComponent;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

import java.io.File;
/**
 * Test new component POJO def
 * @author adaml
 *
 */
public class CloudComponentConfigTest {

	private static final String cloudFilePath = "testResources/cloud/ec2-with-component-config/ec2-cloud.groovy";
	/**
	 * validate that all component properties were read correctly.
	 * @throws Exception
	 */
	@Test
	public void testCloudParse()
			throws Exception {
		Cloud cloud = ServiceReader.readCloud(new File(cloudFilePath));
		GridComponents components = cloud.getConfiguration().getComponents();
		
		validateAgentValues(components.getAgent());
		validateDeployerValues(components.getDeployer());
		validateDiscoveryValues(components.getDiscovery());
		validateOrchestratorValues(components.getOrchestrator());
		validateRestValues(components.getRest());
		validateWebuiValues(components.getWebui());
		validateUsmValues(components.getUsm());
	}
	
	private void validateUsmValues(final UsmComponent usm) {
		Assert.assertTrue("unexpected usm port range property " + usm.getPortRange(), 
				usm.getPortRange().equals("7000-7110"));
		Assert.assertTrue("unexpected usm min memory property " + usm.getMinMemory(),
				usm.getMinMemory().equals("64m"));
		Assert.assertTrue("unexpected usm max memory property " + usm.getMaxMemory(),
				usm.getMaxMemory().equals("1024m"));
		Assert.assertTrue("unexpected usm attributes store discovery timeout property " 
				+ usm.getAttributesStoreDiscoveryTimeoutInSeconds(), 
				usm.getAttributesStoreDiscoveryTimeoutInSeconds() == 30);
		
	}
	private void validateWebuiValues(final WebuiComponent webui) {
		Assert.assertTrue("unexpected webui port property " + webui.getPort(), 
				webui.getPort() == 8101);
		Assert.assertTrue("unexpected webui min memory property " + webui.getMinMemory(),
				webui.getMinMemory().equals("64m"));
		Assert.assertTrue("unexpected webui max memory property " + webui.getMaxMemory(),
				webui.getMaxMemory().equals("64m"));
	}
	
	private void validateRestValues(final RestComponent rest) {
		Assert.assertTrue("unexpected rest port property " + rest.getPort(), 
				rest.getPort() == 8102);
		Assert.assertTrue("unexpected rest min memory property " + rest.getMinMemory(),
				rest.getMinMemory().equals("64m"));
		Assert.assertTrue("unexpected rest max memory property " + rest.getMaxMemory(),
				rest.getMaxMemory().equals("64m"));
		Assert.assertTrue("unexpected rest serviceDiscoveryTimeoutInSeconds property "
				+ rest.getServiceDiscoveryTimeoutInSeconds(),
				rest.getServiceDiscoveryTimeoutInSeconds() == 5);
	}
	
	private void validateOrchestratorValues(final OrchestratorComponent orchestrator) {
		Assert.assertTrue("unexpected orchestrator port property " + orchestrator.getPort(), 
				orchestrator.getPort() == 7779);
		Assert.assertTrue("unexpected orchestrator min memory property " + orchestrator.getMinMemory(),
				orchestrator.getMinMemory().equals("64m"));
		Assert.assertTrue("unexpected orchestrator max memory property " + orchestrator.getMaxMemory(),
				orchestrator.getMaxMemory().equals("4096m"));
        Assert.assertTrue(
                "unexpected orchestrator max memory property " + orchestrator.getStartMachineTimeoutInSeconds(),
                orchestrator.getStartMachineTimeoutInSeconds() == 36000);
        Assert.assertTrue("unexpected orchestrator max memory property " 
                + orchestrator.getStopMachineTimeoutInSeconds(), 
                orchestrator.getStopMachineTimeoutInSeconds() == 36000);
    }
	private void validateDiscoveryValues(final DiscoveryComponent discovery) {
		Assert.assertTrue("unexpected orchestrator discovery port property " + discovery.getDiscoveryPort(), 
				discovery.getDiscoveryPort() == 4174);
		Assert.assertTrue("unexpected orchestrator min memory property " + discovery.getMinMemory(),
				discovery.getMinMemory().equals("64m"));
		Assert.assertTrue("unexpected orchestrator max memory property " + discovery.getMaxMemory(),
				discovery.getMaxMemory().equals("2048m"));
		Assert.assertTrue("unexpected orchestrator lrmi port property " + discovery.getPort(), 
				discovery.getPort() == 2222);
	}
	
	private void validateDeployerValues(final DeployerComponent deployer) {
		Assert.assertTrue("unexpected deployer port property " + deployer.getPort(), 
				deployer.getPort() == 7776);
		Assert.assertTrue("unexpected deployer port property " + deployer.getPort(), 
				deployer.getWebsterPort() == 8888);
		Assert.assertTrue("unexpected deployer min memory property " + deployer.getMinMemory(),
				deployer.getMinMemory().equals("8m"));
		Assert.assertTrue("unexpected deployer max memory property " + deployer.getMaxMemory(),
				deployer.getMaxMemory().equals("16m"));
	}
	
	private void validateAgentValues(final AgentComponent agent) {
		Assert.assertTrue("unexpected deployer port property " + agent.getPort(), 
				agent.getPort() == 7777);
		Assert.assertTrue("unexpected deployer min memory property " + agent.getMinMemory(),
				agent.getMinMemory().equals("32m"));
		Assert.assertTrue("unexpected deployer max memory property " + agent.getMaxMemory(),
				agent.getMaxMemory().equals("128m"));
	}
	
	
	
}
