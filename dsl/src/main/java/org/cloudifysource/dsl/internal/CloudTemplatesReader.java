package org.cloudifysource.dsl.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;

/**
 * A reader for external cloud templates.
 * @author yael
 *
 */
public class CloudTemplatesReader {

	private static Logger logger = Logger.getLogger(CloudTemplatesReader.class.getName()); 

	public CloudTemplatesReader() {

	}

	/**
	 * 
	 * @param templatesZip
	 * 					The templates zipped directory.
	 * @return The templates read from the templates folder un-zipped from templatesZip.
	 * @throws IOException .
	 * @throws DSLException If failed to read the DSL template files.
	 */
	public List<CloudTemplateHolder> readCloudTemplatesFromZip(final File templatesZip) 
			throws IOException, DSLException {
		File templateDirectory = unzipCloudTemplatesFolder(templatesZip);
		return readCloudTemplatesFromDirectory(templateDirectory);
	}
	/**
	 * 
	 * @param templatesZip
	 * 					The templates zipped directory.
	 * @return The template names read from the templates folder un-zipped from templatesZip.
	 * @throws IOException .
	 * @throws DSLException If failed to read the DSL template files.
	 */
	public List<String> getCloudTemplatesNamesFromZip(final File templatesZip) 
			throws IOException, DSLException {
		File templateDirectory = unzipCloudTemplatesFolder(templatesZip);
		List<CloudTemplateHolder> readCloudTemplates = readCloudTemplatesFromDirectory(templateDirectory);
		List<String> templateNames = new ArrayList<String>(readCloudTemplates.size());
		for (CloudTemplateHolder cloudTemplateHolder : readCloudTemplates) {
			templateNames.add(cloudTemplateHolder.getName());
		}
		return templateNames;
	}

	/**
	 * Unzip the templates folder and validate it is a directory.
	 * @param zipFile The file to unzip.
	 * @return The unzipped file.
	 * @throws IOException If failed to unzip the zipFile.
	 */
	public File unzipCloudTemplatesFolder(final File zipFile) 
			throws IOException {
		final File unzipFile = ServiceReader.createTempDir("tempaltes");
		unzipFile.deleteOnExit();
		ZipUtils.unzip(zipFile, unzipFile);
		if (unzipFile.isFile()) {
			throw new IllegalArgumentException("templates folder is not a folder: " + unzipFile.getAbsolutePath());
		}
		return unzipFile;
	}

	/**
	 * 
	 * @param templatesDir 
	 * 						The templates directory.
	 * @return The templates read from the templates files found in templatesDir.
	 * @throws DSLException If failed to read the DSL template files.
	 */
	public List<CloudTemplateHolder> readCloudTemplatesFromDirectory(final File templatesDir) 
			throws DSLException {
		if (!templatesDir.exists()) {
			throw new DSLException(templatesDir + " does not exist.");	
		}
		if (!templatesDir.isDirectory()) {
			throw new DSLException(templatesDir + " is not a directory.");	
		}
		File[] templateFiles =
				DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATES_DSL_FILE_NAME_SUFFIX, templatesDir);
		if (templateFiles == null || templateFiles.length == 0) {
			return new LinkedList<CloudTemplateHolder>();
		}
		Map<String, CloudTemplateHolder> cloudTemplatesMap = new HashMap<String, CloudTemplateHolder>();
		// for each file - reads the templates from it and creates a suitable CloudTemplateHolder object.
		for (File templateFile : templateFiles) {
			List<CloudTemplateHolder> cloudTemplatesFromFile = readCloudTemplatesFromFile(templateFile);
			for (CloudTemplateHolder cloudTemplateHolder : cloudTemplatesFromFile) {
				String name = cloudTemplateHolder.getName();
				if (cloudTemplatesMap.containsKey(name)) {
					throw new DSLException("Template with name [" + name 
							+ "] already exist in folder [" + templatesDir + "]"); 
				}
				cloudTemplatesMap.put(name, cloudTemplateHolder);

			}
		}

		return new LinkedList<CloudTemplateHolder>(cloudTemplatesMap.values());
	}

	/**
	 * 
	 * @param templateFile
	 * 					The template file.
	 * @return The holder of the CloudTemplate read from the file.
	 * @throws DSLException If failed to read the DSL template files..
	 */
	public List<CloudTemplateHolder> readCloudTemplatesFromFile(final File templateFile) 
			throws DSLException {

		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(templateFile);
		dslReader.setCreateServiceContext(false);

		Map<String, CloudTemplate> cloudTemplateMap = dslReader.readDslEntity(Map.class);
		if (cloudTemplateMap.isEmpty()) {
			throw new DSLException("The file " + templateFile + " evaluates to an empty map.");	
		}
		int size = cloudTemplateMap.size();
		if (size > DSLUtils.MAX_TEMPLATES_PER_FILE) {
			throw new DSLException("Too many templates in one groovy file: " + templateFile + " declares " + size 
					+ " templates, only " + DSLUtils.MAX_TEMPLATES_PER_FILE + " allowed.");
		}
		List<CloudTemplateHolder> cloudTemplateHolders = new ArrayList<CloudTemplateHolder>(cloudTemplateMap.size());
		for (Entry<String, CloudTemplate> entry : cloudTemplateMap.entrySet()) {
			CloudTemplateHolder holder = new CloudTemplateHolder();
			holder.setName(entry.getKey());
			holder.setCloudTemplate(entry.getValue());
			holder.setTemplateFileName(templateFile.getName());
			cloudTemplateHolders.add(holder);
		}

		return cloudTemplateHolders;
	}

	/**
	 * Add cloud templates from the template files in the folder.
	 * @param cloud .
	 * @param templatesFolders .
	 */
	public void addAdditionalTemplates(final Cloud cloud, final File[] templatesFolders) {
		List<CloudTemplateHolder> additionalTemplates = null;
		// scan all templates folders and add the templates from each folder to the cloud.
		for (File folder : templatesFolders) {
			try {
				additionalTemplates = readCloudTemplatesFromDirectory(folder);
			} catch (DSLException e) {
				logger.log(Level.WARNING, "addAdditionalTemplates - " + "Failed to read tempaltes from directory: " 
						+ folder.getAbsolutePath() + "Error: " + e.getMessage());
			}
			// scan holders and add all templates to cloud.
			for (CloudTemplateHolder holder : additionalTemplates) {	
				String tempalteName = holder.getName();
				Map<String, CloudTemplate> cloudTemplates = cloud.getTemplates();
				// not supposed to happen
				if (cloudTemplates.containsKey(tempalteName)) {
					logger.log(Level.WARNING, "addAdditionalTemplates - " + "template already exist: " + tempalteName);
				}
				// set the local absolute path to the upload directory
				CloudTemplate cloudTemplate = holder.getCloudTemplate();
				String uploadAbsolutePath = new File(folder, cloudTemplate.getLocalDirectory()).getAbsolutePath();
				cloudTemplate.setAbsoluteUploadDir(uploadAbsolutePath);
				// add template to cloud
				cloudTemplates.put(tempalteName, cloudTemplate);
			}
		}
	}
}
