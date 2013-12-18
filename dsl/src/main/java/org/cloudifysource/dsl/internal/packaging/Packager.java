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
package org.cloudifysource.dsl.internal.packaging;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.BaseDslScript;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;

/************
 * Implementation of the packaging logic required to create a zip file
 * containing the service or application files and additional required files.
 * 
 * @author barakme
 * @since 1.0
 * 
 */
public final class Packager {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(Packager.class.getName());

	private Packager() {

	}

	/*************
	 * Pack a service recipe folder into a zip file.
	 * 
	 * @param recipeDirOrFile
	 *            the recipe directory or recipe file.
	 * @return the packed file.
	 * @throws DSLException
	 * @throws IOException .
	 * @throws PackagingException .
	 * @throws DSLException .
	 */
	public static File pack(final File recipeDirOrFile) throws IOException,
	PackagingException, DSLException {
		return pack(recipeDirOrFile, null);
	}

	/*************
	 * Pack a service recipe folder into a zip file.
	 * 
	 * @param recipeDirOrFile
	 *            the recipe directory or recipe file.
	 * @param additionalServiceFiles
	 *            files to add to the service directory.
	 * @return the packed file.
	 * @throws DSLException
	 * @throws IOException .
	 * @throws PackagingException .
	 * @throws DSLException .
	 */
	public static File pack(final File recipeDirOrFile,
			final List<File> additionalServiceFiles) throws IOException,
			PackagingException, DSLException {
		// Locate recipe file
		final File recipeFile = recipeDirOrFile.isDirectory() ? DSLReader
				.findDefaultDSLFile(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX,
						recipeDirOrFile) : recipeDirOrFile;
				// Parse recipe into service
				final Service service = ServiceReader.readService(recipeFile);
				return pack(recipeFile, false, service, additionalServiceFiles);
	}

	/****************
	 * Packs a service folder.
	 * 
	 * @param recipeDirOrFile
	 *            .
	 * @param service
	 *            .
	 * @param additionalServiceFiles
	 *            files to add to the service directory.
	 * @return the packed file.
	 * @throws PackagingException .
	 * @throws IOException .
	 * @throws DSLException .
	 */
	public static File pack(final File recipeDirOrFile, final Service service,
			final List<File> additionalServiceFiles)
					throws IOException, PackagingException, DSLException {
		if (service == null) {
			return pack(recipeDirOrFile, additionalServiceFiles);
		}
		return pack(recipeDirOrFile, recipeDirOrFile.isDirectory(), service,
				additionalServiceFiles);
	}

	// This method is used by SGTest. Do not change visibility.
	/****************
	 * Packs a service folder.
	 * 
	 * @param recipeDirOrFile
	 *            .
	 * @param isDir
	 *            true if recipeDirOrFile is a Directory.
	 * @param service
	 *            .
	 * @param additionalServiceFiles
	 *            files to add to the service directory.
	 * @return the packed file.
	 * @throws IOException .
	 * @throws PackagingException .
	 */
	public static File pack(final File recipeDirOrFile, final boolean isDir,
			final Service service, final List<File> additionalServiceFiles)
					throws IOException, PackagingException {
		File recipeFile = recipeDirOrFile;
		if (isDir) {
			recipeFile = DSLReader.findDefaultDSLFile(
					DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, recipeDirOrFile);
		}

		if (!recipeFile.isFile()) {
			throw new IllegalArgumentException(recipeFile + " is not a file");
		}

		logger.info("packing folder " + recipeFile.getParent());
		final File createdPuFolder = buildPuFolder(service, recipeFile,
				additionalServiceFiles);
		final File puZipFile = createZippedPu(service, createdPuFolder,
				recipeFile);
		logger.info("created " + puZipFile.getCanonicalFile());
		if (FileUtils.deleteQuietly(createdPuFolder)) {
			logger.finer("deleted temp pu folder "
					+ createdPuFolder.getAbsolutePath());
		}
		return puZipFile;
	}

	/**
	 * Pack the file and name it 'destFileName'.
	 * 
	 * @param service
	 *            .
	 * @param recipeDirOrFile
	 *            Folder or file to pack.
	 * @param destFileName
	 *            The packed file name.
	 * @param additionalServiceFiles
	 *            files to add to the service directory.
	 * @return Packed file named as specified.
	 * @throws DSLException
	 *             DSLException.
	 * @throws IOException
	 *             IOException.
	 * @throws PackagingException
	 *             PackagingException.
	 */
	public static File pack(final Service service, final File recipeDirOrFile,
			final String destFileName, final List<File> additionalServiceFiles)
					throws IOException, PackagingException, DSLException {
		final File packed = pack(recipeDirOrFile, service,
				additionalServiceFiles);
		final File destFile = new File(packed.getParent(), destFileName
				+ ".zip");
		if (destFile.exists()) {
			FileUtils.deleteQuietly(destFile);
		}
		if (packed.renameTo(destFile)) {
			FileUtils.deleteQuietly(packed);
			return destFile;
		}
		logger.info("Failed to rename " + packed.getName() + " to "
				+ destFile.getName());
		return packed;

	}

	private static File createZippedPu(final Service service,
			final File puFolderToZip, final File recipeFile)
					throws IOException, PackagingException {
		logger.finer("trying to zip " + puFolderToZip.getAbsolutePath());
		String name = service.getName();
		final String serviceName = name != null ? name : recipeFile.getParentFile().getName();

		// create a temp dir under the system temp dir
		final File tmpFile = File.createTempFile("ServicePackage", null);
		tmpFile.delete();
		tmpFile.mkdir();

		final File zipFile = new File(tmpFile, serviceName + ".zip");

		// files will be deleted in reverse order
		tmpFile.deleteOnExit();
		zipFile.deleteOnExit();

		ServiceReader
		.validateFolderSize(puFolderToZip, service.getMaxJarSize());
		ZipUtils.zip(puFolderToZip, zipFile);
		logger.finer("zipped folder successfully to "
				+ zipFile.getAbsolutePath());
		return zipFile;
	}

	/**
	 * source folder structure: service.groovy something.zip install.sh start.sh
	 * ...
	 * <p/>
	 * usmlib mylib1.jar mylib2.jar ...
	 * <p/>
	 * output folder: ext service.groovy something.zip install.sh start.sh ...
	 * lib mylib1.jar mylib2.jar ... usm.jar
	 * <p/>
	 * META-INF spring pu.xml
	 * 
	 * @param srcFolder
	 * @param recipeDirOrFile
	 * @return
	 * @throws IOException
	 * @throws PackagingException
	 */
	private static File buildPuFolder(final Service service,
			final File recipeFile, final List<File> additionalServiceFiles)
					throws IOException, PackagingException {
		final File srcFolder = recipeFile.getParentFile();
		final File destPuFolder = File.createTempFile("gs_usm_", "");
		FileUtils.forceDelete(destPuFolder);
		FileUtils.forceMkdir(destPuFolder);
		logger.finer("created temp directory " + destPuFolder.getAbsolutePath());

		// create folders
		final File extFolder = new File(destPuFolder, "/ext");
		FileUtils.forceMkdir(extFolder);
		final File libFolder = new File(destPuFolder.getAbsolutePath(), "/lib");
		FileUtils.forceMkdir(libFolder);
		final File springFolder = new File(destPuFolder.getAbsolutePath(),
				"/META-INF/spring");
		FileUtils.forceMkdir(springFolder);

		logger.finer("created pu structure under " + destPuFolder);

		FileUtils.copyDirectory(srcFolder, extFolder);
		// Copy additional files to service directory
		if (additionalServiceFiles != null) {
			for (final File file : additionalServiceFiles) {
				FileUtils.copyFileToDirectory(file, extFolder);
			}
		}

		logger.finer("copied files from " + srcFolder.getAbsolutePath()
				+ " to " + extFolder.getAbsolutePath());

		// copy all files from usmlib to lib
		final File srcUsmLibDir = new File(srcFolder, "usmlib");
		if (srcUsmLibDir.exists()) {
			FileUtils.copyDirectory(srcUsmLibDir, libFolder,
					SVNFileFilter.getFilter());
		}

		// copy usm.jar to lib
		// final File usmLibDir = getUsmLibDir(service);
		// final File srcUsmJar = new File(usmLibDir, "usm.jar");
		// if (!srcUsmJar.exists()) {
		// throw new PackagingException("could not find usm.jar at: " +
		// srcUsmJar);
		// }
		// FileUtils
		// .copyDirectory(usmLibDir, libFolder, SVNFileFilter.getFilter());
		// logger.finer("copied " + srcUsmJar.getName());

		// no pu.xml in source folder, lets copy the default one
		final InputStream puXmlStream = Packager.class.getClassLoader()
				.getResourceAsStream("META-INF/spring/default_usm_pu.xml");
		if (puXmlStream == null) {
			throw new PackagingException("can not find locate default pu.xml");
		}
		final File destPuXml = new File(springFolder, "pu.xml");
		FileUtils.copyInputStreamToFile(puXmlStream, destPuXml);
		logger.finer("copied pu.xml");
		try {
			puXmlStream.close();
		} catch (final IOException e) {
			logger.log(Level.SEVERE,
					"failed to close default_usm_pu.xml stream", e);
		}

		copyExtendedServiceFiles(service, recipeFile, extFolder);

		createManifestFile(destPuFolder);

		logger.finer("created pu folder " + destPuFolder.getAbsolutePath());
		return destPuFolder;
	}

	private static void createManifestFile(final File destPuFolder)
			throws IOException {
		final File manifestFolder = new File(destPuFolder, "META-INF");
		final File manifestFile = new File(manifestFolder, "MANIFEST.MF");

		final Manifest manifest = new Manifest();

		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue("Class-Path",
				  "lib/platform/cloudify/dsl.jar "
            + "lib/platform/cloudify/domain.jar "
            + "lib/platform/cloudify/dsl-backwards.jar "
            + "lib/platform/cloudify/domain.jar "
				+ "lib/platform/cloudify/utility-domain.jar " 
				+ "lib/platform/usm/usm.jar "
				// added support for @grab annotation in groovy file - requires ivy and groovy in same classloader
				+ "tools/groovy/embeddable/groovy-all-1.8.6.jar "
            + "tools/groovy/lib/ivy-2.2.0.jar ");

		OutputStream out = null;

		try {
			out = new BufferedOutputStream(new FileOutputStream(manifestFile));
			manifest.write(out);

		} finally {
			if (out != null) {

				try {
					out.close();
				} catch (final IOException e) {
					logger.log(Level.SEVERE, "Failed to close file: "
							+ manifestFile, e);
				}

			}
		}

	}

	/*************
	 * .
	 * 
	 * @see org.cloudifysource.dsl.internal.packaging.Packager.packApplication(
	 *      Application, File, File[])
	 * @param application
	 *            .
	 * @param applicationDir
	 *            .
	 * @return .
	 * @throws IOException .
	 * @throws PackagingException .
	 */
	public static File packApplication(final Application application,
			final File applicationDir) throws IOException, PackagingException {
		return packApplication(application, applicationDir, null);
	}

	/***************
	 * Packs an application folder into a zip file.
	 * 
	 * @param application
	 *            the application object as read from the application file.
	 * @param applicationDir
	 *            the directory where the application was read from.
	 * @param additionalServiceFiles
	 *            additional files that should be packaged into each service
	 *            directory.
	 * @return the packaged zip file.
	 * @throws IOException
	 *             IOException.
	 * @throws PackagingException
	 *             PackagingException.
	 */
	public static File packApplication(final Application application, final File applicationDir,
			final List<File> additionalServiceFiles) throws IOException,
			PackagingException {

		boolean hasExtendedServices = false;
		for (final Service service : application.getServices()) {
			if (!service.getExtendedServicesPaths().isEmpty()) {
				hasExtendedServices = true;
				break;
			}
		}
		File applicationFolderToPack = applicationDir;
		// If there are no extended service we don't need to prepare an
		// application folder to pack with all the
		// extended services content.
		if (hasExtendedServices) {
			final File destApplicationFolder = createCopyDirectory(applicationFolderToPack);

			for (final Service service : application.getServices()) {
				final File extFolder = new File(destApplicationFolder + "/"
						+ service.getName());
				final File recipeFile = DSLReader.findDefaultDSLFile(
						DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, new File(
								applicationDir + "/" + service.getName()));
				copyExtendedServiceFiles(service, recipeFile, extFolder);
			}
			// Pack the prepared folder instead of the original application
			// folder.
			applicationFolderToPack = destApplicationFolder;
		}

		if ((additionalServiceFiles != null)
				&& (!additionalServiceFiles.isEmpty())) {
			// if a copy directory was already created, use the existing one,
			// otherwise
			// create a new one.
			if (applicationFolderToPack == applicationDir) {
				applicationFolderToPack = createCopyDirectory(applicationFolderToPack);
			}
			final List<Service> services = application.getServices();
			for (final Service service : services) {
				final File serviceDir = new File(applicationFolderToPack,
						service.getName());
				if (!serviceDir.exists()) {
					throw new PackagingException(
							"Could not find service folder at: " + serviceDir);
				}
				if (!serviceDir.isDirectory()) {
					throw new PackagingException(
							"Was expecting a directory at: " + serviceDir);
				}

				for (final File fileToCopy : additionalServiceFiles) {
					FileUtils.copyFileToDirectory(fileToCopy, serviceDir);
				}
			}
		}

		// zip the application folder.
		return createZipFile("application", applicationFolderToPack);
	}

	/**
	 * 
	 * @param zipFileName
	 *            The name of the zip file.
	 * @param packedDir
	 *            The directory to pack.
	 * @return The packaged zip file.
	 * @throws IOException .
	 */
	public static File createZipFile(final String zipFileName, final File packedDir)
			throws IOException {
		String shortName = zipFileName;
		if (zipFileName.endsWith(".zip")) {
			shortName = zipFileName.split("//.zip")[0];
		}
		final File zipFile = File.createTempFile(shortName, ".zip");
		zipFile.deleteOnExit();
		ZipUtils.zip(packedDir, zipFile);
		logger.finer("zipped folder successfully to " + zipFile.getAbsolutePath());
		return zipFile;
	}

	private static File createCopyDirectory(final File applicationDir)
			throws IOException {
		final File destApplicationFolder = File.createTempFile(
				"gs_application_", "");
		FileUtils.forceDelete(destApplicationFolder);
		FileUtils.forceMkdir(destApplicationFolder);
		FileUtils.copyDirectory(applicationDir, destApplicationFolder,
				SVNFileFilter.getFilter());
		return destApplicationFolder;
	}

	private static void copyExtendedServiceFiles(final Service service,
			final File recipeFile, final File extFolder)
					throws IOException {
		final LinkedList<String> extendedServicesPaths = service
				.getExtendedServicesPaths();

		File extendingScriptFile = new File(extFolder + "/"
				+ recipeFile.getName());
		File currentExtendedServiceContext = recipeFile;

		for (final String extendedServicePath : extendedServicesPaths) {
			// Locate the extended service file in the destination path
			final File extendedServiceFile = locateServiceFile(
					currentExtendedServiceContext, extendedServicePath);
			// If the extended service exists in my directory, no need to copy
			// or change anything
			// This can happen if we have extension of services inside
			// application since the client
			// will prepare the extending service directory already and then it
			// will be prepared fully at the server
			if (extendedServiceFile.getParentFile().equals(
					recipeFile.getParentFile())) {
				continue;
			}
			// Copy it to local dir with new name if needed
			final File localExtendedServiceFile = copyExtendedServiceFileAndRename(
					extendedServiceFile, extFolder);
			logger.finer("copying locally extended script "
					+ extendedServiceFile + " to " + localExtendedServiceFile);
			// Update the extending script extend property with the location of
			// the new extended service script
			updateExtendingScriptFileWithNewExtendedScriptLocation(
					extendingScriptFile, localExtendedServiceFile);
			// Copy remote resources locally
			final File rootScriptDir = extendedServiceFile.getParentFile();
			FileUtils.copyDirectory(rootScriptDir, extFolder, new FileFilter() {

				@Override
				public boolean accept(final File pathname) {
					if (!SVNFileFilter.getFilter().accept(pathname)) {
						return false;
					}
					if (pathname.equals(extendedServiceFile)) {
						return false;
					}
					if (pathname.isDirectory()) {
						return true;
					}
					final String relativePath = pathname.getPath().replace(
							rootScriptDir.getPath(), "");
					final boolean accept = !new File(extFolder.getPath() + "/"
							+ relativePath).exists();
					if (accept && logger.isLoggable(Level.FINEST)) {
						logger.finest("copying extended script resource ["
								+ pathname + "] locally");
					}
					return accept;

				}
			});
			// Replace context extending script file for multiple level
			// extension
			extendingScriptFile = localExtendedServiceFile;
			currentExtendedServiceContext = extendedServiceFile;
		}
	}

	private static void updateExtendingScriptFileWithNewExtendedScriptLocation(
			final File extendingScriptFile, final File localExtendedServiceFile)
					throws IOException {
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		final File extendingScriptFileTmp = new File(extendingScriptFile
				.getPath().replace(".groovy", "-tmp.groovy"));
		try {
			bufferedReader = new BufferedReader(new FileReader(
					extendingScriptFile));
			final FileWriter fileWriter = new FileWriter(extendingScriptFileTmp);
			bufferedWriter = new BufferedWriter(fileWriter);
			String line = bufferedReader.readLine();
			while (line != null) {
				if (line.trim().startsWith(
						BaseDslScript.EXTEND_PROPERTY_NAME + " ")) {
					line = line
							.substring(
									0,
									line.indexOf(BaseDslScript.EXTEND_PROPERTY_NAME)
									+ BaseDslScript.EXTEND_PROPERTY_NAME.length());
					line += " \"" + localExtendedServiceFile.getName() + "\"";
				}
				bufferedWriter.write(line + System.getProperty("line.separator"));
				line = bufferedReader.readLine();
			}
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		}
		FileUtils.forceDelete(extendingScriptFile);
		if (!extendingScriptFileTmp.renameTo(extendingScriptFile)) {
			throw new IOException("Failed renaming tmp script ["
					+ extendingScriptFileTmp + "] to [" + extendingScriptFile
					+ "]");
		}
	}

	private static File copyExtendedServiceFileAndRename(
			final File extendedServiceFile, final File extFolder)
					throws IOException {
		final File existingServiceFile = new File(extFolder + "/"
				+ extendedServiceFile.getName());
		// We need to locate the next available index as it may be there was
		// multi layer extension
		final int index = locateNextAvailableScriptIndex(existingServiceFile);
		// Generate a new name for the service script with the new available
		// index
		final String existingServiceFilePath = existingServiceFile.getPath();
		final String nestedExtendedServiceFileName = existingServiceFilePath
				+ "-" + index;
		final File destFile = new File(nestedExtendedServiceFileName);
		// Copy extended script
		FileUtils.copyFile(extendedServiceFile, destFile);
		return destFile;
	}

	private static int locateNextAvailableScriptIndex(
			final File extendedServiceFile) {
		int index = 1;
		while (true) {
			if (!new File(extendedServiceFile.getPath() + "-" + index).exists()) {
				return index;
			}
			index++;
		}
	}

	private static File locateServiceFile(final File recipeFile,
			final String extendedServicePath) {
		File extendedServiceFile = new File(extendedServicePath);
		if (!extendedServiceFile.isAbsolute()) {
			extendedServiceFile = new File(recipeFile.getParent() + "/"
					+ extendedServicePath);
		}
		if (extendedServiceFile.isDirectory()) {
			extendedServiceFile = DSLReader
					.findDefaultDSLFile(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX,
							extendedServiceFile);
		}

		return extendedServiceFile;
	}

}
