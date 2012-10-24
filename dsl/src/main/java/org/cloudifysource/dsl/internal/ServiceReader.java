/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.openspaces.admin.Admin;
import org.openspaces.core.cluster.ClusterInfo;

/****************
 * Utility methods for commonly used DSL parsers.
 * 
 * @author barakme
 * @since 2.0
 * 
 */
public final class ServiceReader {

	/*******
	 * name of property injected into DSL context containing the path to the DSL file.
	 */
	public static final String DSL_FILE_PATH_PROPERTY_NAME = "dslFilePath";

	/*****
	 * Private Constructor to prevent instantiation.
	 * 
	 */
	private ServiceReader() {

	}

	public static File extractProjectFile(final File projectZipFile)
			throws IOException {

		File destFolder = null;
		destFolder = File.createTempFile("gs_usm_", "");
		FileUtils.forceDelete(destFolder);
		FileUtils.forceMkdir(destFolder);

		ZipUtils.unzip(projectZipFile, destFolder);

		return destFolder;

	}

	public static void validateFolderSize(final File serviceDirOrFile, final long maxJarSizePermitted)
			throws PackagingException {
		File folder = serviceDirOrFile;
		if (folder.isFile()) {
			folder = folder.getParentFile();
		}
		final long folderSize = FileUtils.sizeOfDirectory(folder);
		if (folderSize == 0) {
			throw new PackagingException("folder " + folder.getAbsolutePath() + " is empty");
		}
		final long maxJarSize = maxJarSizePermitted;
		if (folderSize > maxJarSize || folderSize == 0) {
			throw new PackagingException("folder " + folder.getAbsolutePath() + "size is: "
					+ FileUtils.byteCountToDisplaySize(folderSize) + ", it must be smaller than: "
					+ FileUtils.byteCountToDisplaySize(maxJarSize));
		}

	}

	public static Service getServiceFromFile(final File dslFile)
			throws PackagingException {
		try {
			return ServiceReader.getServiceFromFile(dslFile,
					// new File(System.getProperty("user.dir"))).getService();
					dslFile.getParentFile()).getService();
		} catch (final CompilationFailedException e) {
			throw new PackagingException("The file " + dslFile + " could not be compiled: " + e.getMessage(), e);
		} catch (final Exception e) {
			throw new PackagingException("Failed to read service from file " + dslFile + ": " + e.getMessage(), e);
		}
	}

	/****************
	 * Reads a service object from a groovy DSL file placed in the given directory. The file name must be of the format
	 * *-service.groovy, and there must be exactly one file in the directory with a name that matches this format.
	 * 
	 * @param dir the directory to scan for the DSL file.
	 * @param applicationName
	 * @return the service
	 * @throws PackagingException
	 * @throws FileNotFoundException
	 * @throws DSLException
	 */
	public static DSLServiceCompilationResult getServiceFromDirectory(final File dir, final String applicationName)
			throws FileNotFoundException, PackagingException, DSLException {
		return ServiceReader.getServiceFromFile(null, dir, null, null, null, true);

	}

	public static DSLServiceCompilationResult getServiceFromFile(final File dslFile, final File workDir)
			throws DSLException {
		return ServiceReader.getServiceFromFile(dslFile, workDir, null, null, null, true);
	}

	public static DSLServiceCompilationResult getServiceFromFile(final File dslFile, final File workDir,
			final Admin admin, final ClusterInfo clusterInfo, final String propertiesFileName,
			final boolean isRunningInGSC)
			throws DSLException {

		final DSLReader dslReader = new DSLReader();
		dslReader.setAdmin(admin);
		dslReader.setClusterInfo(clusterInfo);
		dslReader.setPropertiesFileName(propertiesFileName);
		dslReader.setRunningInGSC(isRunningInGSC);
		dslReader.setDslFile(dslFile);
		dslReader.setWorkDir(workDir);
		dslReader.setDslFileNameSuffix(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX);

		final Service service = dslReader.readDslEntity(Service.class);

		return new DSLServiceCompilationResult(service, dslReader.getContext(), dslFile);
	}

	public static DSLApplicationCompilatioResult getApplicationFromFile(final File inputFile)
			throws IOException,
			DSLException {

		File actualApplicationDslFile = inputFile;

		if (inputFile.isFile()) {
			if (inputFile.getName().endsWith(".zip") || inputFile.getName().endsWith(".jar")) {
				// Unzip application zip file to temp folder
				final File tempFolder = ServiceReader.unzipApplicationFile(inputFile);
				actualApplicationDslFile =
						DSLReader.findDefaultDSLFile(DSLReader.APPLICATION_DSL_FILE_NAME_SUFFIX, tempFolder);
			}
		} else {
			actualApplicationDslFile =
					DSLReader.findDefaultDSLFile(DSLReader.APPLICATION_DSL_FILE_NAME_SUFFIX, inputFile);
		}

		final Application app = ServiceReader.readApplicationFromFile(actualApplicationDslFile);

		return new DSLApplicationCompilatioResult(app, actualApplicationDslFile.getParentFile(),
				actualApplicationDslFile);

	}

	private static File unzipApplicationFile(final File inputFile)
			throws IOException {

		final File baseDir = ServiceReader.createTempDir();

		ZipUtils.unzip(inputFile, baseDir);
		return baseDir;

	}

	public static Service readServiceFromZip(final File inputFile, final String applicationName)
			throws IOException,
			PackagingException, DSLException {
		final File projectFolder = extractProjectFile(inputFile);
		try {
			return ServiceReader.getServiceFromDirectory(projectFolder, applicationName).getService();
		} finally {
			FileUtils.forceDelete(projectFolder);
		}

	}

	public static final void copyInputStream(final InputStream in, final OutputStream out)
			throws IOException {
		final byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}

	protected static File createTempDir()
			throws IOException {
		final File tempFile = File.createTempFile("GS_tmp_dir", ".application");
		final String path = tempFile.getAbsolutePath();
		tempFile.delete();
		tempFile.mkdirs();
		final File baseDir = new File(path);
		return baseDir;
	}
	
	private static Application readApplicationFromFile(final File dslFile)
			throws IOException, DSLException {

		final DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());

		final Application application = dslReader.readDslEntity(Application.class);

		return application;

	}

	private static Cloud readCloud(final String dslContents, final File dslFile)
			throws DSLException {

		final DSLReader dslReader = new DSLReader();

		dslReader.setCreateServiceContext(false);
		dslReader.setDslContents(dslContents);
		dslReader.setDslFile(dslFile);
		if (dslFile != null) {
			dslReader.setWorkDir(dslFile.getParentFile());
		}

		final Cloud cloud = dslReader.readDslEntity(Cloud.class);
		return cloud;
	}

	public static org.cloudifysource.dsl.cloud.Cloud readCloud(final File dslFile)
			throws IOException,
			DSLException {

		if (!dslFile.exists()) {
			throw new FileNotFoundException(dslFile.getAbsolutePath());
		}

		final String dslContents = FileUtils.readFileToString(dslFile);

		final Cloud cloud = readCloud(dslContents, dslFile);
		return cloud;
	}

	public static Service readService(final File dslFileOrDir)
			throws PackagingException, DSLException {
		if (dslFileOrDir.isFile()) {
			return getServiceFromFile(dslFileOrDir);
		} else if (dslFileOrDir.isDirectory()) {
			return ServiceReader.getServiceFromFile(null, dslFileOrDir, null, null, null, true).getService();
		} else {
			throw new IllegalArgumentException(dslFileOrDir + " is neither a file nor a directory");
		}

	}

	public static Cloud readCloudFromDirectory(final String cloudConfigDirectory)
			throws DSLException {
		final DSLReader reader = new DSLReader();
		reader.setDslFileNameSuffix(DSLReader.CLOUD_DSL_FILE_NAME_SUFFIX);
		reader.setWorkDir(new File(cloudConfigDirectory));
		reader.setCreateServiceContext(false);
		final Cloud cloud = reader.readDslEntity(Cloud.class);
		return cloud;
	}
}
