package org.cloudifysource.utilitydomain.data.reader;

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

import org.cloudifysource.domain.ComputeTemplateHolder;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
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
			throw new DSLException("There is no template files (files with the suffix " 
					+ DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX + ") in the given folder [" + templatesDir + "]");
		}
		Map<String, ComputeTemplateHolder> cloudTemplatesMap = new HashMap<String, ComputeTemplateHolder>();
		// for each file - reads the templates from it and creates a suitable CloudTemplateHolder object.
		for (File templateFile : templateFiles) {
			List<ComputeTemplateHolder> cloudTemplatesFromFile;
			try {
				cloudTemplatesFromFile = readCloudTemplatesFromFile(templateFile);
			} catch (Exception e) {
				throw new DSLException("Failed to read template file [" + templateFile.getName()
						+ "] from folder [" + templatesDir.getAbsolutePath() + "]. Reason: " + e.getMessage(), e);
			}
			for (ComputeTemplateHolder cloudTemplateHolder : cloudTemplatesFromFile) {
				String name = cloudTemplateHolder.getName();
				if (cloudTemplatesMap.containsKey(name)) {
					throw new DSLException("Failed to read template file [" + templateFile.getName()
						+ "] from folder [" + templatesDir.getAbsolutePath() 
						+ "]. Reason: template with the name [" + name + "] already exist in folder.");
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
			throw new DSLException("Too many templates in one groovy file [" + templateFile.getName() + " declares " + size
					+ " templates, only " + DSLUtils.MAX_TEMPLATES_PER_FILE + " allowed].");
		}
		List<ComputeTemplateHolder> cloudTemplateHolders = 
				new ArrayList<ComputeTemplateHolder>(cloudTemplateMap.size());
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
			logger.log(Level.INFO, 
					"[addAdditionalTemplates] - Adding templates to cloud from folder " + folder.getAbsolutePath());
			logger.info("[addAdditionalTemplates] - Folder's files: " + Arrays.toString(folder.listFiles()));
			try {
				additionalTemplates = readCloudTemplatesFromDirectory(folder);
				// scan holders and add all templates to cloud.
				for (ComputeTemplateHolder holder : additionalTemplates) {
					String templateName = holder.getName();
					Map<String, ComputeTemplate> cloudTemplates = cloud.getCloudCompute().getTemplates();
					// not supposed to happen
					if (cloudTemplates.containsKey(templateName)) {
						logger.log(Level.WARNING, "[addAdditionalTemplates] - template already exist: " + templateName);
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
			} catch (DSLException e) {
				logger.log(Level.WARNING, "[addAdditionalTemplates] - Failed to read templates from directory ["
						+ folder.getAbsolutePath() + "]. Error: " + e.getMessage());
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
			boolean delete = propertiesFile.delete();
			logger.log(Level.FINE, "[removeTemplateFiles] - " 
					+ (delete ? "Successfully deleted" : "Failed to delete")
					+ " template's properties file [" + propertiesFile.getAbsolutePath() + "].");
		}
		String overridesFileName = templateName + DSLUtils.TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX;
		File overridesFile = new File(templateFolder, overridesFileName);
		if (overridesFile.exists()) {
			boolean delete = overridesFile.delete();
			logger.log(Level.FINE, "[removeTemplateFiles] - " 
					+ (delete ? "Successfully deleted" : "Failed to delete")
					+ " template's overrides file [" + overridesFile.getAbsolutePath() + "].");
		}
	}
}
