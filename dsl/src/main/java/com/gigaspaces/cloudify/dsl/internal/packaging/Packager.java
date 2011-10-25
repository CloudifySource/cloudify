package com.gigaspaces.cloudify.dsl.internal.packaging;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.j_spaces.kernel.Environment;

public class Packager {

	public final static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(Packager.class.getName());

	public static final String USM_JAR_PATH_PROP = "usmJarPath";

	public static File pack(final File recipeDirOrFile) throws IOException, PackagingException {
		Service service = ServiceReader.readService(recipeDirOrFile);
		File recipeDir = recipeDirOrFile;
		if (recipeDir.isFile()) {
			recipeDir = recipeDir.getParentFile();
		}
		return Packager.pack(recipeDir, service);
	}
	
	public static File pack(final File srcFolder, final Service service) throws IOException, PackagingException {
		if (!srcFolder.isDirectory()) {
			throw new IllegalArgumentException (srcFolder + " is not a directory");
		}
		logger.info("packing folder " + srcFolder);
		final File createdPuFolder = buildPuFolder(service, srcFolder);
		File puZipFile = createZippedPu(service, createdPuFolder, srcFolder);
		logger.info("created " + puZipFile.getCanonicalFile());
		if (FileUtils.deleteQuietly(createdPuFolder)) {
			logger.finer("deleted temp pu folder " + createdPuFolder.getAbsolutePath());
		}
		return puZipFile;
	}

	private static File createZippedPu(Service service, final File puFolderToZip, final File srcFolder) throws IOException, PackagingException {
		logger.finer("trying to zip " + puFolderToZip.getAbsolutePath());
		String serviceName = (service.getName() != null ? service.getName() : srcFolder.getName());
		
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
	 * @return
	 * @throws IOException
	 * @throws PackagingException
	 */
	private static File buildPuFolder(Service service, final File srcFolder) throws IOException, PackagingException {
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
			logger.log(Level.SEVERE, "failed to close defaul_usm_pu.xml stream", e);
		}
		logger.finer("created pu folder " + destPuFolder.getAbsolutePath());
		return destPuFolder;
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
