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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.BaseDslScript;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.ServiceReader;

import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

public class Packager {

	public final static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(Packager.class.getName());

	public static final String USM_JAR_PATH_PROP = "usmJarPath";

	public static File pack(final File recipeDirOrFile) throws IOException, PackagingException, DSLException {
		//Locate recipe file
		File recipeFile = recipeDirOrFile.isDirectory()? DSLReader.findDefaultDSLFile(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX, recipeDirOrFile) : recipeDirOrFile;
		//Parse recipe into service
		Service service = ServiceReader.readService(recipeFile);
		return Packager.pack(recipeFile, service);
	}
	
	/**
	 * Pack the file and name it 'destFileName'
	 * @param recipeDirOrFile - Folder or file to pack.
	 * @param destFileName - The packed file name.
	 * @return Packed file named as specified.
	 * @throws IOException
	 * @throws PackagingException
	 * @throws DSLException 
	 */
	public static File pack(final File recipeDirOrFile, String destFileName) throws IOException, PackagingException, DSLException {
		File packed = pack(recipeDirOrFile);
		File destFile = new File(packed.getParent(), destFileName + ".zip");
		if (destFile.exists()){
			FileUtils.deleteQuietly(destFile);
		}
		if (packed.renameTo(destFile)){
			FileUtils.deleteQuietly(packed);
			return destFile;
		}
		logger.info("Failed to rename " 
				+ packed.getName() 
				+ " to " 
				+ destFile.getName());
		return packed;
		
	}
	
	//This method is being used by SGTest. Do not change visibility.
	public static File pack(final File recipeFile, final Service service) throws IOException, PackagingException {
		if (!recipeFile.isFile())
			throw new IllegalArgumentException (recipeFile + " is not a file");
		
		logger.info("packing folder " + recipeFile.getParent());
		final File createdPuFolder = buildPuFolder(service, recipeFile);
		File puZipFile = createZippedPu(service, createdPuFolder, recipeFile);
		logger.info("created " + puZipFile.getCanonicalFile());
		if (FileUtils.deleteQuietly(createdPuFolder)) {
			logger.finer("deleted temp pu folder " + createdPuFolder.getAbsolutePath());
		}
		return puZipFile;
	}

	private static File createZippedPu(Service service, final File puFolderToZip, final File recipeFile) throws IOException, PackagingException {
		logger.finer("trying to zip " + puFolderToZip.getAbsolutePath());
		String serviceName = (service.getName() != null ? service.getName() : recipeFile.getParentFile().getName());
		
		// create a temp dir under the system temp dir
		File tmpFile = File.createTempFile("ServicePackage", null);
		tmpFile.delete();
		tmpFile.mkdir();
		
		final File zipFile = new File(tmpFile, serviceName + ".zip");
		
		// files will be deleted in reverse order
		tmpFile.deleteOnExit();
		zipFile.deleteOnExit();
		
		ServiceReader.validateFolderSize(puFolderToZip, service.getMaxJarSize());
		ZipUtils.zip(puFolderToZip, zipFile);
		logger.finer("zipped folder successfully to " + zipFile.getAbsolutePath());
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
	private static File buildPuFolder(Service service, final File recipeFile) throws IOException, PackagingException {
		final File srcFolder = recipeFile.getParentFile();
		File destPuFolder = File.createTempFile("gs_usm_", "");
		FileUtils.forceDelete(destPuFolder);
		FileUtils.forceMkdir(destPuFolder);
		logger.finer("created temp directory " + destPuFolder.getAbsolutePath());

		// create folders
		final File extFolder = new File(destPuFolder, "/ext");
		FileUtils.forceMkdir(extFolder);
		final File libFolder = new File(destPuFolder.getAbsolutePath(), "/lib");
		FileUtils.forceMkdir(libFolder);
		final File springFolder = new File(destPuFolder.getAbsolutePath(), "/META-INF/spring");
		FileUtils.forceMkdir(springFolder);
		logger.finer("created pu structure under " + destPuFolder);

		// copy all files except usmlib from working dir to ext
//		FileUtils.copyDirectory(srcFolder, extFolder, new FileFilter() {
//
//			@Override
//			public boolean accept(File pathname) {
//				boolean f1 = SVNFileFilter.getFilter().accept(pathname);
//				boolean f2 = !(pathname.isDirectory() && pathname.getName().equals("usmlib"));
//				return f1 && f2;
//			}
//		});
		FileUtils.copyDirectory(srcFolder, extFolder);
		
		logger.finer("copied files from " + srcFolder.getAbsolutePath() + " to " + extFolder.getAbsolutePath());

		// copy all files from usmlib to lib
		File srcUsmLibDir = new File(srcFolder, "usmlib");
		if (srcUsmLibDir.exists()) {
			FileUtils.copyDirectory(srcUsmLibDir, libFolder, SVNFileFilter.getFilter());
		}

		// copy usm.jar to lib
		final File usmLibDir = getUsmLibDir(service);
		final File srcUsmJar = new File(usmLibDir, "usm.jar");
		if (!srcUsmJar.exists()) {
			throw new PackagingException("could not find usm.jar");
		}
		FileUtils.copyDirectory(usmLibDir, libFolder, SVNFileFilter.getFilter());
		logger.finer("copied " + srcUsmJar.getName());

		// no pu.xml in source folder, lets copy the default one
		final InputStream puXmlStream =
				Packager.class.getClassLoader().getResourceAsStream("META-INF/spring/default_usm_pu.xml");
		if (puXmlStream == null) {
			throw new PackagingException("can not find locate default pu.xml");
		}
		final File destPuXml = new File(springFolder, "pu.xml");
		FileUtils.copyInputStreamToFile(puXmlStream, destPuXml);
		logger.finer("copied pu.xml");
		try {
			puXmlStream.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "failed to close default_usm_pu.xml stream", e);
		}
		
		copyExtendedServiceFiles(service, recipeFile, extFolder);
		
		logger.finer("created pu folder " + destPuFolder.getAbsolutePath());
		return destPuFolder;
	}

	public static File packApplication(Application application, File applicationDir) throws IOException, PackagingException {
		
		boolean hasExtendedServices = false;
		for (Service service : application.getServices()) {
			if (!service.getExtendedServicesPaths().isEmpty()){
				hasExtendedServices = true;
				break;
			}
		}
		File applicationFolderToPack = applicationDir;
		//If there are no extended service we don't need to prepare an application folder to pack with all the
		//extended services content.
		if (hasExtendedServices) {
			File destApplicationFolder = File.createTempFile("gs_application_", "");
			FileUtils.forceDelete(destApplicationFolder);
			FileUtils.forceMkdir(destApplicationFolder);
			FileUtils.copyDirectory(applicationDir, destApplicationFolder, SVNFileFilter.getFilter());	
			
			for (Service service : application.getServices()) {
				File extFolder = new File(destApplicationFolder + "/" + service.getName());
				File recipeFile = DSLReader.findDefaultDSLFile(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX, new File(applicationDir + "/" + service.getName()));
				copyExtendedServiceFiles(service, recipeFile, extFolder);
			}
			//Pack the prepared folder instead of the original application folder.
			applicationFolderToPack = destApplicationFolder;
		}
		
		// zip the application folder.
		File zipFile = File.createTempFile("application", ".zip");
		zipFile.deleteOnExit();
		ZipUtils.zip(applicationFolderToPack, zipFile);
		return zipFile;
	}
	
	private static void copyExtendedServiceFiles(Service service,
			final File recipeFile, final File extFolder) throws FileNotFoundException,
			PackagingException, IOException {
		LinkedList<String> extendedServicesPaths = service.getExtendedServicesPaths();
		
		File extendingScriptFile = new File(extFolder + "/" + recipeFile.getName());
		File currentExtendedServiceContext = recipeFile;
		
		for (String extendedServicePath : extendedServicesPaths) {
			//Locate the extended service file in the destination path
			final File extendedServiceFile = locateServiceFile(currentExtendedServiceContext, extendedServicePath);
			//If the extended service exists in my directory, no need to copy or change anything
			//This can happen if we have extension of services inside application since the client
			//will prepare the extending service directory already and then it will be prepared fully at the server
			if (extendedServiceFile.getParentFile().equals(recipeFile.getParentFile()))
				continue;
			//Copy it to local dir with new name if needed
			File localExtendedServiceFile = copyExtendedServiceFileAndRename(extendedServiceFile, extFolder);
			logger.finer("copying locally extended script " + extendedServiceFile + " to " + localExtendedServiceFile);
			//Update the extending script extend property with the location of the new extended service script
			updateExtendingScriptFileWithNewExtendedScriptLocation(extendingScriptFile, localExtendedServiceFile);
			//Copy remote resources locally
			final File rootScriptDir = extendedServiceFile.getParentFile();
			FileUtils.copyDirectory(rootScriptDir, extFolder, new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if (!SVNFileFilter.getFilter().accept(pathname))
						return false;
					if (pathname.equals(extendedServiceFile))
						return false;
					if (pathname.isDirectory())
						return true;
					String relativePath = pathname.getPath().replace(rootScriptDir.getPath(), "");
					boolean accept = !(new File(extFolder.getPath() + "/" + relativePath).exists());
					if (accept && logger.isLoggable(Level.FINEST))
						logger.finest("copying extended script resource [" + pathname + "] locally");
					return accept;
						
				}
			});
			//Replace context extending script file for multiple level extension
			extendingScriptFile = localExtendedServiceFile;
			currentExtendedServiceContext = extendedServiceFile;
		}
	}

	private static void updateExtendingScriptFileWithNewExtendedScriptLocation(
			File extendingScriptFile, File localExtendedServiceFile) throws IOException {
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		File extendingScriptFileTmp = new File(extendingScriptFile.getPath().replace(".groovy", "-tmp.groovy"));
		try{
			bufferedReader = new BufferedReader(new FileReader(extendingScriptFile));
			FileWriter fileWriter = new FileWriter(extendingScriptFileTmp);
			bufferedWriter = new BufferedWriter(fileWriter);
			String line = bufferedReader.readLine();
			while(line != null){
				if (line.trim().startsWith(BaseDslScript.EXTEND_PROPERTY_NAME + " ")){
					line = line.substring(0, line.indexOf(BaseDslScript.EXTEND_PROPERTY_NAME) + 6);
					line += " \"" + localExtendedServiceFile.getName() + "\"";
				}
				bufferedWriter.write(line + StringUtils.NEW_LINE);
				line = bufferedReader.readLine();
			}
		}
		finally{
			if (bufferedReader != null)
				bufferedReader.close();
			if (bufferedWriter != null)
				bufferedWriter.close();
		}
		FileUtils.forceDelete(extendingScriptFile);
		if (!extendingScriptFileTmp.renameTo(extendingScriptFile))
			throw new IOException("Failed renaming tmp script [" + extendingScriptFileTmp + "] to [" + extendingScriptFile +"]");
	}

	private static File copyExtendedServiceFileAndRename(
			File extendedServiceFile, File extFolder) throws IOException {
		File existingServiceFile = new File(extFolder + "/" + extendedServiceFile.getName());
		//We need to locate the next available index as it may be there was multi layer extension
		final int index = locateNextAvailableScriptIndex(existingServiceFile);
		//Generate a new name for the service script with the new available index
		final String existingServiceFilePath = existingServiceFile.getPath();
		String nestedExtendedServiceFileName = existingServiceFilePath + "-" + index;
		File destFile = new File(nestedExtendedServiceFileName);
		//Copy extended script
		FileUtils.copyFile(extendedServiceFile, destFile);
		return destFile;
	}

	private static int locateNextAvailableScriptIndex(File extendedServiceFile) {
		int index = 1;
		while(true){
			if (!(new File(extendedServiceFile.getPath() + "-"+ index).exists()))
				return index;
			index++;
		}
	}

	private static File locateServiceFile(File recipeFile, String extendedServicePath) throws FileNotFoundException, PackagingException {
		File extendedServiceFile = new File(extendedServicePath);
		if (!extendedServiceFile.isAbsolute())
		    extendedServiceFile = new File(recipeFile.getParent() + "/" + extendedServicePath);
		if (extendedServiceFile.isDirectory())
			extendedServiceFile = DSLReader.findDefaultDSLFile(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX,extendedServiceFile);
		
		return extendedServiceFile;
	}

	/**
	 * @return
	 */
	private static File getUsmLibDir(Service service) {
		Map<String, String> customProperties = service.getCustomProperties();
		File usmLibDir = null;
		if (customProperties != null) {
			String usmJarPathProp = customProperties.get(USM_JAR_PATH_PROP);
			if (usmJarPathProp != null) {
				usmLibDir = new File(usmJarPathProp);
			}
		}
		if (usmLibDir == null) {
			usmLibDir = new File(Environment.getHomeDirectory() + "/lib/platform/usm");
		}
		return usmLibDir;
	}

}
