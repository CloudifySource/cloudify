package org.openspaces.usm.container;

//import java.io.IOException;
//
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.openspaces.core.cluster.ClusterInfo;
//import org.openspaces.pu.container.ProcessingUnitContainer;
//import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;

/*******
 * Initial work on USM init test framework.
 * @author barakme
 *
 */
public class SimpleUSMTest {


//	@BeforeClass
//	public static void init() {
//		final String cloudifyHome = System.getenv("CLOUDIFY_HOME");
//		if(cloudifyHome == null) {
//			throw new IllegalStateException("Environment variable CLOUDIFY_HOME is missing");
//		}
//
//		System.setProperty("org.hyperic.sigar.path", cloudifyHome + "/lib/platform/sigar");
//		System.setProperty("com.gs.home", cloudifyHome);
//
//	}
//	@Test
//	public void simpleTest() throws IOException {
//
//		IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
//		// provide cluster information for the specific PU instance
//		ClusterInfo clusterInfo = new ClusterInfo();
//		//clusterInfo.setSchema("partitioned-sync2backup");
//		clusterInfo.setNumberOfInstances(1);
//		clusterInfo.setInstanceId(1);
//		provider.setClusterInfo(clusterInfo);
//
//		// set the config location (override the default one - classpath:/META-INF/spring/pu.xml)
//		provider.addConfigLocation("classpath:/simple/META-INF/spring/pu.xml");
//
//		// Build the Spring application context and "start" it
//		ProcessingUnitContainer container = provider.createContainer();
//
//		// ...
//
//		container.close();
//	}
}
