/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.installer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;

/**************
 * Wrapper for the logic required to switch the previous deployment of management services with new versions following a
 * re-bootstrapping of the cloud managers with persistence enabled.
 *
 * @author barakme
 *
 */
public class ManagementRedeployer {

	private static final Logger logger = Logger.getLogger(ManagementRedeployer.class.getName());

	private boolean restRedeployed = false;
	private boolean webuiRedeployed = false;

	/******
	 * Executed the management redeployment logic.
	 *
	 * @param persistencePath
	 *            the persistence folder configured for this cloud.
	 * @param cloudifyHomePath
	 *            the cloudify home directory.
	 * @throws IOException
	 *             in case of an error.
	 */
	public void run(final String persistencePath, final String cloudifyHomePath) throws IOException {

		// first get the persistence path
		if (persistencePath == null) {
			return; // not a persistent cloud
		}

		// check it is valid
		final File persistenceDir = new File(persistencePath);
		if (persistenceDir.exists() && !persistenceDir.isDirectory()) {
			throw new IllegalStateException("The persistence location: " + persistencePath
					+ " should either not exist or be a directory");
		}

		// if it does not exist, must be first bootstrap
		if (!persistenceDir.exists()) {
			// must be first bootstrap
			return;
		}

		final File deployDir =
				new File(persistenceDir, CloudifyConstants.PERSISTENCE_DIRECTORY_DEPLOY_RELATIVE_PATH);
		if (!deployDir.exists()) {
			// must be first bootstrap
			return;
		}

		final File restDir = findServiceDeployDir(deployDir, CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME);
		final File webuiDir = findServiceDeployDir(deployDir, CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME);

		if (!restDir.exists() && !webuiDir.exists()) {
			// maybe someone created the directory structure first? Either way, nothing to redeploy.
			return;
		}
		if (restDir.exists() ^ webuiDir.exists()) { // Hah - XOR!
			logger.warning("Found only one of the management service deployed: REST: "
					+ restDir.exists() + ", Web-UI: " + webuiDir.exists() + ". "
					+ "This is not a normal deployment scenario. Please check for configuration problems. "
					+ "Only the deployed service will be installed");
		}

		if (restDir.exists()) {
			logger.info("Updating REST service deployment");
			final File restFile = findRestDeploymentFile(cloudifyHomePath);
			switchDeploymentContent(restDir, restFile);
			restRedeployed = true;
		}

		if (webuiDir.exists()) {
			logger.info("Updating Web-UI service deployment");
			final File webuiFile = findWebuiDeploymentFile(cloudifyHomePath);
			switchDeploymentContent(webuiDir, webuiFile);
			webuiRedeployed = true;
		}

	}

    private File findServiceDeployDir(final File deployDir, final String serviceName) {

        File[] files = deployDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.contains(serviceName);
            }
        });
        if (files.length != 1) {
            throw new IllegalStateException("Expected to find 1 directory that contains '" + serviceName + "' in folder '" + deployDir.getAbsolutePath() + "'. but found : " + files.length);
        }
        return files[0];
    }

	private void switchDeploymentContent(final File targetDir, final File sourceFile) throws IOException {
		FileUtils.deleteDirectory(targetDir);
		targetDir.mkdirs();
		ZipUtils.unzip(sourceFile, targetDir);
	}

	private File findRestDeploymentFile(final String cloudifyHome) {
		final String restSourcePath = cloudifyHome + "/tools/rest";
		final File file = findWarFile(restSourcePath);
		return file;

	}

	private File findWebuiDeploymentFile(final String cloudifyHome) {
		final String restSourcePath = cloudifyHome + "/tools/gs-webui";
		final File file = findWarFile(restSourcePath);
		return file;

	}

	private File findWarFile(final String restSourcePath) {
		final File restSourceDir = new File(restSourcePath);
		File[] files = restSourceDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(".war");
			}
		});
		if (files.length == 0) {
			throw new IllegalStateException("Excepted to find a single war file in: " + restSourcePath
					+ ", but found none");
		}
		if (files.length > 1) {
			throw new IllegalStateException("Excepted to find a single war file in: " + restSourcePath
					+ ", but found: " + files);
		}
		return files[0];
	}

	public boolean isRestRedeployed() {
		return restRedeployed;
	}

	public boolean isWebuiRedeployed() {
		return webuiRedeployed;
	}

}
