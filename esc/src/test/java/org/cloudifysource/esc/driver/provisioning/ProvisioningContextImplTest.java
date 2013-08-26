package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudProvider;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.RemoteExecutionModes;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.util.InstallationDetailsBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ProvisioningContextImplTest {

	private static final String HTTP_JAVA_URL = "http://java.url";
	private static final String TEMPLATE_NAME = "default";

	@Test
	public void testJavaExist() throws FileNotFoundException {
		Cloud cloud = new Cloud();
		cloud.setProvider(new CloudProvider());
		cloud.getProvider().setManagementOnlyFiles(new ArrayList<String>());
		ComputeTemplate template = new ComputeTemplate();
		template.setJavaUrl(HTTP_JAVA_URL);
		cloud.getConfiguration().setManagementMachineTemplate(TEMPLATE_NAME);
		cloud.getCloudCompute().setTemplates(new LinkedHashMap<String, ComputeTemplate>());
		cloud.getCloudCompute().getTemplates().put(TEMPLATE_NAME, template);
		cloud.getConfiguration().getComponents().getRest().setPort(8100);
		cloud.getConfiguration().getComponents().getWebui().setPort(8099);
		cloud.getConfiguration().getComponents().getDiscovery().setDiscoveryPort(4172);

		final ProvisioningContextImpl ctx = new ProvisioningContextImpl();
		ctx.setLocationId(null);
		InstallationDetailsBuilder builder = ctx.getInstallationDetailsBuilder();
		builder.setReservationId(null);
		builder.setAdmin(null);

		builder.setAuthGroups(null);
		builder.setCloud(cloud);
		builder.setCloudFile(new File("some-cloud.groovy"));
		builder.setKeystorePassword(null);
		builder.setLookupLocators(null);
		builder.setManagement(true);
		builder.setRebootstrapping(false);
		builder.setReservationId(null);
		builder.setSecurityProfile("");
		builder.setTemplate(cloud.getCloudCompute().getTemplates()
				.get(cloud.getConfiguration().getManagementMachineTemplate()));
		builder.setTemplateName(cloud.getConfiguration().getManagementMachineTemplate());
		builder.setZones(new HashSet<String>(Arrays.asList("management")));

		MachineDetails md = new MachineDetails();
		md.setAgentRunning(true);
		md.setFileTransferMode(FileTransferModes.SFTP);
		md.setPrivateAddress("10.10.10.10");
		md.setPublicAddress("20.20.20.20");
		md.setRemoteExecutionMode(RemoteExecutionModes.SSH);
		
		final MachineDetails[] mds = new MachineDetails[] {md};
		final String[] results = ctx.createManagementEnvironmentScript(mds, template);
		
		Assert.assertEquals(1, results.length);
		final String result = results[0];
		System.out.println(result);
		Assert.assertTrue("java url not found", result.contains(HTTP_JAVA_URL));
		

		
	}
}
