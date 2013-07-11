package org.cloudifysource.esc.installer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.esc.installer.filetransfer.ScpFileTransfer;
import org.junit.Ignore;
import org.junit.Test;

public class FileTransferTest {

	/*******
	 * This is a test framework for file transfers. It turned out really hard to run an embedded ssh server that will do
	 * what we need, so this test has been disabled.
	 *
	 * @throws IOException .
	 * @throws TimeoutException .
	 * @throws InstallerException .
	 */
	@Ignore
	@Test
	public void testScpFileTransfer() throws IOException, TimeoutException, InstallerException {
		ScpFileTransfer transfer = new ScpFileTransfer();
		InstallationDetails details = new InstallationDetails();

		details.setAdmin(null);
		details.setBindToPrivateIp(true);
		details.setConnectedToPrivateIp(true);
		details.setDeleteRemoteDirectoryContents(true);
		details.setFileTransferMode(FileTransferModes.SCP);
		details.setLocalDir("c:/temp/temp");
		details.setManagement(false);

		// TODO
		details.setPassword("reverse");
		details.setPrivateIp("192.168.0.6");
		details.setPublicIp("BLABLA");
		details.setRemoteDir("/home/ubuntu/transfer");
		details.setUsername("ubuntu");

		final long end = System.currentTimeMillis() + 1000 * 60;

		transfer.initialize(details, end);
		transfer.copyFiles(details, new HashSet<String>(Arrays.asList("2.txt", "subfolder1", "subfolder2/1.txt")),
				new ArrayList<File>(), end);

	}

}
