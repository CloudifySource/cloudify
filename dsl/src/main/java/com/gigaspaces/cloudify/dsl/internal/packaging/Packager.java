package com.gigaspaces.cloudify.dsl.internal.packaging;

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

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.BaseDslScript;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

public class Packager {

	public final static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(Packager.class.getName());

	public static final String USM_JAR_PATH_PROP = "usmJarPath";

	public static File pack(final File recipeDirOrFile) throws IOException, PackagingException {
		//Locate recipe file
		File recipeFile = recipeDirOrFile.isDirectory()? ServiceReader.findServiceFile(recipeDirOrFile) : recipeDirOrFile;
		//Parse recipe into service
		Service service = ServiceReader.readService(recipeFile);
		return Packager.pack(recipeFile, service);
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

		final SVNFileFilter svnFileFilter = new SVNFileFilter();
		// copy all files except usmlib from working dir to ext
		FileUtils.copyDirectory(srcFolder, extFolder, new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				boolean f1 = svnFileFilter.accept(pathname);
				boolean f2 = !(pathname.isDirectory() && pathname.getName().equals("usmlib"));
				return f1 && f2;
			}
		});
		logger.finer("copied files from " + srcFolder.getAbsolutePath() + " to " + extFolder.getAbsolutePath());

		// copy all files from usmlib to lib
		File srcUsmLibDir = new File(srcFolder, "usmlib");
		if (srcUsmLibDir.exists()) {
			FileUtils.copyDirectory(srcUsmLibDir, libFolder, svnFileFilter);
		}

		// copy usm.jar to lib
		final File usmLibDir = getUsmLibDir(service);
		final File srcUsmJar = new File(usmLibDir, "usm.jar");
		if (!srcUsmJar.exists()) {
			throw new PackagingException("could not find usm.jar");
		}
		FileUtils.copyDirectory(usmLibDir, libFolder, svnFileFilter);
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
		
		LinkedList<String> extendedServicesPaths = service.getExtendedServicesPaths();
		
		File extendingScriptFile = new File(extFolder + "/" + recipeFile.getName());
		
		for (String extendedServicePath : extendedServicesPaths) {
			//Locate the extended service file in the destination path
			File extendedServiceFile = locateServiceFile(recipeFile, extendedServicePath);
			//Copy it to local dir with new name if needed
			File localExtendedServiceFile = copyServiceFileAndRenameIfNeeded(extendedServiceFile, extFolder);
			logger.finer("copying locally extended script " + extendedServiceFile + " to " + localExtendedServiceFile);
			//Update the extending script extend property with the location of the new extended service script
			updateExtendingScriptFileWithNewExtendedScriptLocation(extendingScriptFile, localExtendedServiceFile);
			//Copy remote resources locally
			final File rootScriptDir = extendedServiceFile.getParentFile();
			FileUtils.copyDirectory(rootScriptDir, extFolder, new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
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
		}
		
		logger.finer("created pu folder " + destPuFolder.getAbsolutePath());
		return destPuFolder;
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

	private static File copyServiceFileAndRenameIfNeeded(
			File extendedServiceFile, File extFolder) throws IOException {
		File existingServiceFile = new File(extFolder + "/" + extendedServiceFile.getName());
		if (!existingServiceFile.exists())
		{
			FileUtils.copyFileToDirectory(extendedServiceFile, extFolder);
			return existingServiceFile;
		}
		else
		{
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
		File extendedServiceFile = new File(recipeFile.getParent() + "/" + extendedServicePath);
		if (extendedServiceFile.isDirectory())
			extendedServiceFile = ServiceReader.findServiceFile(extendedServiceFile);
		
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

	public static class SVNFileFilter implements FileFilter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File pathname) {
			return !pathname.getName().equals(".svn");
		}

	}

}
