package org.cloudifysource.esc.installer;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.cloudifysource.dsl.cloud.ScriptLanguages;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.openspaces.admin.gsa.GSAReservationId;


/*******
 * Unit tests for the agentless installer. These are hard to execute as they require the
 * right environment to run. We could try to set up an embedded ssh server to simulate. Not
 * much of an option for powershell, though.
 *
 * @author barakme
 *
 */
public class InstallerTest {

	private static final String LINUX_USERNAME = "xxxxx";
	private static final String WINDOWS_PASSWORD = "xxxxx";
	private static final String WINDOWS_USERNAME = "xxxx";
	private static final String LINUX_PASSWORD = "xxxxxx";

	@Ignore
	@Test
	public void testPowershellLocal() throws TimeoutException, InterruptedException, InstallerException {

		AgentlessInstaller installer = new AgentlessInstaller();
		InstallationDetails details = new InstallationDetails();

		details.setAdmin(null);
		details.setBindToPrivateIp(true);
		details.setCloudFile(new File(System.getenv("CLOUDIFY_HOME")
				+ "/tools/cli/plugins/esc/byon-local/byon-cloud.groovy"));
		details.setCloudifyUrl("http://localhost:8090/cloudify/gigaspaces");
		details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_JAVA_URL,
				"http://localhost:8090/cloudify/java.zip");
		details.setConnectedToPrivateIp(true);
		details.setFileTransferMode(FileTransferModes.CIFS);
		details.setLocalDir(System.getenv("CLOUDIFY_HOME") + "/tools/cli/plugins/esc/byon-local/upload-win");
		details.setLocator(null);
		details.setManagement(true);
		details.setMachineId("TEST_NODE");
		details.setRelativeLocalDir("upload-win");

		details.setRemoteDir("/C$/gs-files");
		details.setRemoteExecutionMode(RemoteExecutionModes.WINRM);
		details.setReservationId(new GSAReservationId("TEST_ID"));

		details.setScriptLanguage(ScriptLanguages.WINDOWS_BATCH);
		details.setTemplateName("TEST_TEMPLATE");

		details.setUsername(WINDOWS_USERNAME);
		details.setPassword(WINDOWS_PASSWORD);
		details.setPrivateIp("localhost");

		details.setDeleteRemoteDirectoryContents(true);
		installer.addListener(new AgentlessInstallerListener() {

			@Override
			public void onInstallerEvent(String eventName, Object... args) {
				System.out.println("Event: " + eventName + ", Parameters: " + Arrays.asList(args));

			}
		});

		installer.installOnMachineWithIP(details, 5, TimeUnit.MINUTES);

	}

	@Test
	@Ignore
	public void testSShLocal() throws TimeoutException, InterruptedException, InstallerException {

		AgentlessInstaller installer = new AgentlessInstaller();
		InstallationDetails details = new InstallationDetails();

		details.setAdmin(null);
		details.setBindToPrivateIp(true);
		details.setCloudFile(new File(System.getenv("CLOUDIFY_HOME") + "/tools/cli/plugins/esc/ec2/ec2-cloud.groovy"));
		details.setCloudifyUrl("http://localhost:8090/cloudify/gigaspaces.zip");
		details.setConnectedToPrivateIp(true);
		details.setFileTransferMode(FileTransferModes.SFTP);
		details.setLocalDir(System.getenv("CLOUDIFY_HOME") + "/tools/cli/plugins/esc/ec2/upload");
		details.setLocator(null);
		details.setManagement(true);
		details.setMachineId("TEST_NODE");
		details.setRelativeLocalDir("upload");

		details.setRemoteDir("/tmp/gs-files");
		details.setRemoteExecutionMode(RemoteExecutionModes.SSH);
		details.setReservationId(new GSAReservationId("TEST_ID"));

		details.setScriptLanguage(ScriptLanguages.LINUX_SHELL);
		details.setTemplateName("TEST_TEMPLATE");

		details.setUsername(LINUX_USERNAME);
		details.setPassword(LINUX_PASSWORD);
		details.setPrivateIp("localhost");

		installer.addListener(new AgentlessInstallerListener() {

			@Override
			public void onInstallerEvent(String eventName, Object... args) {
				System.out.println("Event: " + eventName + ", Parameters: " + Arrays.asList(args));

			}
		});

		installer.installOnMachineWithIP(details, 5, TimeUnit.MINUTES);

	}
}
