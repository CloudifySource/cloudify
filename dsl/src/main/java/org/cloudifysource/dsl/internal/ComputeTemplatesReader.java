package org.cloudifysource.dsl.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;

/**
 * A reader for external cloud templates.
 * @author yael
 *
 */
public class ComputeTemplatesReader {

	private static Logger logger = Logger.getLogger(ComputeTemplatesReader.class.getName());

	public ComputeTemplatesReader() {

	}

	/**
	 *
	 * @param templatesZip
	 * 					The templates zipped directory.
	 * @return The templates read from the templates folder un-zipped from templatesZip.
	 * @throws IOException .
	 * @throws DSLException If failed to read the DSL template files.
	 */
	public List<ComputeTemplateHolder> readCloudTemplatesFromZip(final File templatesZip)
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
		List<ComputeTemplateHolder> readCloudTemplates = readCloudTemplatesFromDirectory(templateDirectory);
		List<String> templateNames = new ArrayList<String>(readCloudTemplates.size());
		for (ComputeTemplateHolder cloudTemplateHolder : readCloudTemplates) {
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
		final File unzipFile = ServiceReader.createTempDir("templates");
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
	public List<ComputeTemplateHolder> readCloudTemplatesFromDirectory(final File templatesDir)
			throws DSLException {
		if (!templatesDir.exists()) {
			throw new DSLException(templatesDir + " does not exist.");
		}
		if (!templatesDir.isDirectory()) {
			throw new DSLException(templatesDir + " is not a directory.");
		}
		File[] templateFiles =
				DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, templatesDir);
		if (templateFiles == null || templateFiles.length == 0) {
			return new LinkedList<ComputeTemplateHolder>();
		}
		Map<String, ComputeTemplateHolder> cloudTemplatesMap = new HashMap<String, ComputeTemplateHolder>();
		// for each file - reads the templates from it and creates a suitable CloudTemplateHolder object.
		for (File templateFile : templateFiles) {
			List<ComputeTemplateHolder> cloudTemplatesFromFile = readCloudTemplatesFromFile(templateFile);
			for (ComputeTemplateHolder cloudTemplateHolder : cloudTemplatesFromFile) {
				String name = cloudTemplateHolder.getName();
				if (cloudTemplatesMap.containsKey(name)) {
					throw new DSLException("Template with name [" + name
							+ "] already exist in folder [" + templatesDir + "]");
				}
				cloudTemplatesMap.put(name, cloudTemplateHolder);

			}
		}

		return new LinkedList<ComputeTemplateHolder>(cloudTemplatesMap.values());
	}

	/**
	 *
	 * @param templateFile
	 * 					The template file.
	 * @return The holder of the CloudTemplate read from the file.
	 * @throws DSLException If failed to read the DSL template files..
	 */
	public List<ComputeTemplateHolder> readCloudTemplatesFromFile(final File templateFile)
			throws DSLException {

		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(templateFile);
		dslReader.setCreateServiceContext(false);

		@SuppressWarnings("unchecked")
		Map<String, ComputeTemplate> cloudTemplateMap = dslReader.readDslEntity(Map.class);
		if (cloudTemplateMap.isEmpty()) {
			throw new DSLException("The file " + templateFile + " evaluates to an empty map.");
		}
		int size = cloudTemplateMap.size();
		if (size > DSLUtils.MAX_TEMPLATES_PER_FILE) {
			throw new DSLException("Too many templates in one groovy file: " + templateFile + " declares " + size
					+ " templates, only " + DSLUtils.MAX_TEMPLATES_PER_FILE + " allowed.");
		}
		List<ComputeTemplateHolder> cloudTemplateHolders = new ArrayList<ComputeTemplateHolder>(cloudTemplateMap.size());
		for (Entry<String, ComputeTemplate> entry : cloudTemplateMap.entrySet()) {
			ComputeTemplateHolder holder = new ComputeTemplateHolder();
			holder.setName(entry.getKey());
			holder.setCloudTemplate(entry.getValue());
			holder.setTemplateFileName(templateFile.getName());
			holder.setPropertiesFileName(dslReader.getPropertiesFileName());
			File overridesFile = dslReader.getOverridesFile();
			if (overridesFile != null) {
				holder.setOverridesFileName(overridesFile.getName());
			}
			cloudTemplateHolders.add(holder);
		}

		return cloudTemplateHolders;
	}

	/**
	 * Add cloud templates from the template files in the folder.
	 * @param cloud .
	 * @param templatesFolders .
	 * @return the added templates.
	 */
	public List<ComputeTemplate> addAdditionalTemplates(final Cloud cloud, final File[] templatesFolders) {
		List<ComputeTemplate> addedTemplates = new LinkedList<ComputeTemplate>();
		List<ComputeTemplateHolder> additionalTemplates = null;
		// scan all templates folders and add the templates from each folder to the cloud.
		for (File folder : templatesFolders) {
			logger.info("addAdditionalTemplates - Adding templates to cloud from folder: " + folder.getAbsolutePath());
			logger.info("addAdditionalTemplates - Folder's files: " + Arrays.toString(folder.listFiles()));
			try {
				additionalTemplates = readCloudTemplatesFromDirectory(folder);
			} catch (DSLException e) {
				logger.log(Level.WARNING, "addAdditionalTemplates - " + "Failed to read templates from directory: "
						+ folder.getAbsolutePath() + "Error: " + e.getMessage());
			}
			// scan holders and add all templates to cloud.
			for (ComputeTemplateHolder holder : additionalTemplates) {
				String templateName = holder.getName();
				Map<String, ComputeTemplate> cloudTemplates = cloud.getCloudCompute().getTemplates();
				// not supposed to happen
				if (cloudTemplates.containsKey(templateName)) {
					logger.log(Level.WARNING, "addAdditionalTemplates - " + "template already exist: " + templateName);
					continue;
				}
				// set the local absolute path to the upload directory
				ComputeTemplate cloudTemplate = holder.getCloudTemplate();
				String uploadAbsolutePath = new File(folder, cloudTemplate.getLocalDirectory()).getAbsolutePath();
				cloudTemplate.setAbsoluteUploadDir(uploadAbsolutePath);
				// add template to cloud
				cloudTemplates.put(templateName, cloudTemplate);
				addedTemplates.add(cloudTemplate);
			}
		}
		return addedTemplates;
	}

	/**
	 * Removes template file with a given suffix from templateFolder.
	 * @param templateFolder .
	 * @param templateName .
	 */
	public static void removeTemplateFiles(final File templateFolder, final String templateName) {
		String proeprtiesFileName = templateName + DSLUtils.TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX;
		File propertiesFile = new File(templateFolder, proeprtiesFileName);
		if (propertiesFile.exists()) {
			propertiesFile.delete();
		}
		String overridesFileName = templateName + DSLUtils.TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX;
		File overridesFile = new File(templateFolder, overridesFileName);
		if (overridesFile.exists()) {
			overridesFile.delete();
		}
	}
}
