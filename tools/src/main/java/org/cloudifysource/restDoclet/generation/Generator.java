package org.cloudifysource.restDoclet.generation;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.docElements.DocAnnotation;
import org.cloudifysource.restDoclet.docElements.DocController;
import org.cloudifysource.restDoclet.docElements.DocHttpMethod;
import org.cloudifysource.restDoclet.docElements.DocMethod;
import org.cloudifysource.restDoclet.docElements.DocParameter;
import org.cloudifysource.restDoclet.docElements.DocPossibleResponseStatusesAnnotation;
import org.cloudifysource.restDoclet.docElements.DocRequestMappingAnnotation;
import org.cloudifysource.restDoclet.docElements.DocReturnDetails;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

/**
 * Generates REST API documentation in an HTML form.
 * <br />
 * Uses velocity template to generate an html file that contains the documentation.
 * <ul>
 * <li>To specify your sources change the values of {@link RestDocConstants#SOURCE_PATH_FLAG} flag in the {@link #main(String[])}.</li>
 * <li>To generate the documentation using a different {@link Doclet} class, create a new {@link Generator} and call the {@link #run()} method from the {@link Doclet#start(RootDoc)} method.</li>
 * <br />
 * <li>To change the template path add the flag {@link RestDocConstants#VELOCITY_TEMPLATE_PATH_FLAG} with the wanted path.</li>
 * <br />
 * In default the Generator uses the velocity template {@link RestDocConstants#DEFAULT_VELOCITY_TEMPLATE_PATH}
 * and writes the result to {@link RestDocConstants#DEFAULT_DOC_DEST_PATH}.
 * <li>To change the destination of the result html add the flag {@link RestDocConstants#DOC_DEST_PATH_FLAG} with the wanted path.
 * <br />
 * <li>To generate the documentation in a different way, you can do one of the following:</li>
 * <ul><li>Override the {@link #generateHtmlDocumentation(List)} method - this way you have to generate a <code>String</code> documentation.</li>
 * <li>Extend the {@link Generator} class, override the {@link #run()} method and call the {@link #generateControllers(ClassDoc[])} method to get the list of {@link DocController}s. 
 * <br />Then you can use the {@link DocController}s' list to generate your documentation as you wish.</li></ul>
 * </ul>
 * @author yael
 * 
 */
public class Generator {
	private RootDoc documentation;
	private String velocityTemplatePath;
	private String velocityTemplateFileName;
	private boolean isUserDefineTemplatePath = false;
	private String docPath;
	private String version;
	private static final Logger logger = Logger.getLogger(Generator.class.getName());


	/**
	 * 
	 * @param rootDoc
	 */
	public Generator(final RootDoc rootDoc) {
		documentation = rootDoc;
		setFlags(documentation.options());
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		com.sun.tools.javadoc.Main.execute(new String[] { 
				RestDocConstants.DOCLET_FLAG, RestDoclet.class.getName(), 
				RestDocConstants.SOURCE_PATH_FLAG, RestDocConstants.SOURCES_PATH, RestDocConstants.CONTROLLERS_PACKAGE,
//				RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG, RestDocConstants.DEFAULT_VELOCITY_TEMPLATE_PATH,
//				RestDocConstants.DOC_DEST_PATH_FLAG, "target/test.html",
//				RestDocConstants.VERSION_FLAG, "2.2.0",
		});
	}

	/**
	 * 
	 * @param options
	 */
	private void setFlags(String[][] options) {
		for (int i = 0; i < options.length; i++) {
			if (options[i][0].equals(RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG)) {
				velocityTemplatePath = options[i][1];
				isUserDefineTemplatePath = true;
			}
			else if (options[i][0].equals(RestDocConstants.DOC_DEST_PATH_FLAG)) 
				docPath = options[i][1];
			else if(options[i][0].equals(RestDocConstants.VERSION_FLAG))
				version = options[i][1];
		}

		if(velocityTemplatePath != null) {
			int fileNameIndex = velocityTemplatePath.lastIndexOf("/") + 1;
			velocityTemplateFileName = velocityTemplatePath.substring(fileNameIndex);
			velocityTemplatePath = velocityTemplatePath.substring(0, fileNameIndex - 1);
		}
		else {
			velocityTemplateFileName = RestDocConstants.DEFAULT_VELOCITY_TEMPLATE_FILE_NAME;
			velocityTemplatePath = this.getClass().getClassLoader().getResource(velocityTemplateFileName).getPath();
		}

		if(docPath == null)
			docPath =  RestDocConstants.DEFAULT_DOC_DEST_PATH;
		
		if(version == null)
			version = "";
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {

		// GENERATE DOCUMENTATIONS IN DOC CLASSES
		List<DocController> controllers = generateControllers(documentation.classes());

		Utils.printMethodsToFile(controllers, "methods");
		
		// TRANSLATE DOC CLASSES INTO HTML DOCUMENTATION USING VELOCITY TEMPLATE
		String generatedHtml = generateHtmlDocumentation(controllers);

		// WRITE GENERATED HTML TO A FILE
		FileWriter velocityfileWriter = null;
		try {
			File file = new File(docPath);
			if(file.getParentFile() != null)
				file.getParentFile().mkdirs();
			logger.log(Level.INFO, "Write generated velocity to " + file.getAbsolutePath());
			velocityfileWriter = new FileWriter(file);
			velocityfileWriter.write(generatedHtml);
		} finally {
			if (velocityfileWriter != null)
				velocityfileWriter.close();
		}
	}

	/**
	 * Creates the REST API documentation in HTML form,
	 * using the controllers' data and the velocity template.
	 * @param controllers
	 * @return string that contains the documentation in HTML form.
	 * @throws Exception
	 */
	public String generateHtmlDocumentation(List<DocController> controllers) throws Exception {

		logger.log(Level.INFO, "Generate velocity using template: "
				+ velocityTemplatePath
				+ " (" + (isUserDefineTemplatePath
						? "got template path from user"
						: "default template path") + ")");

		Properties p = new Properties();
		if (isUserDefineTemplatePath) {
			p.setProperty("file.resource.loader.path", velocityTemplatePath);
		}
		else {
			p.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			p.setProperty("classpath.resource.loader.class",ClasspathResourceLoader.class.getName());
		}

		Velocity.init(p);

		VelocityContext ctx = new VelocityContext();

		ctx.put("controllers", controllers);
		ctx.put("version", version);

		Writer writer = new StringWriter();
		
		Template template = Velocity.getTemplate(velocityTemplateFileName);
		template.merge(ctx, writer);
		
		return writer.toString();

	}

	private List<DocController> generateControllers(ClassDoc[] classes) {
		List<DocController> controllers = new LinkedList<DocController>();
		for (ClassDoc classDoc : classes) {
			DocController controller = generateController(classDoc);
			if (controller == null)
				continue;
			controllers.add(controller);
		}
		return controllers;
	}

	private DocController generateController(ClassDoc classDoc) {
		DocController controller = new DocController(classDoc.typeName());

		List<DocAnnotation> annotations = generateAnnotations(classDoc
				.annotations());
		if (Utils.filterOutControllerClass(classDoc, annotations))
			return null;

		controller.setUri(Utils.getRequestMappingAnnotation(annotations)
				.getValue());
		controller.setMethods(generateMethods(classDoc.methods()));
		controller.setDescription(classDoc.commentText());

		if (StringUtils.isBlank(controller.getUri()))
			throw new IllegalArgumentException("controller class "
					+ controller.getName()
					+ " is missing request mapping annotation's value (uri).");
		if (controller.getMethods().isEmpty())
			throw new IllegalArgumentException("controller class "
					+ controller.getName() + " doesn't have methods.");

		return controller;
	}

	private List<DocAnnotation> generateAnnotations(AnnotationDesc[] annotations) {
		List<DocAnnotation> docAnnotations = new LinkedList<DocAnnotation>();
		for (AnnotationDesc annotationDesc : annotations) {
			DocAnnotation docAnnotation = Utils
					.createNewAnnotation(annotationDesc.annotationType()
							.typeName());
			// add annotation's attributes
			for (ElementValuePair elementValuePair : annotationDesc
					.elementValues()) {
				String element = elementValuePair.element().toString();
				Object constractAttrValue = docAnnotation
						.constractAttrValue(elementValuePair.value().value());
				docAnnotation.addAttribute(element, constractAttrValue);
			}
			docAnnotations.add(docAnnotation);
		}
		return docAnnotations;
	}

	private SortedMap<String, DocMethod> generateMethods(MethodDoc[] methods) {
		SortedMap<String, DocMethod> docMethods = new TreeMap<String, DocMethod>();

		for (MethodDoc methodDoc : methods) {
			List<DocAnnotation> annotations = generateAnnotations(methodDoc
					.annotations());
			DocRequestMappingAnnotation requestMappingAnnotation = Utils
					.getRequestMappingAnnotation(annotations);

			if (requestMappingAnnotation == null)
				continue;

			DocHttpMethod httpMethod = generateHttpMethod(methodDoc,
					requestMappingAnnotation.getMethod(), annotations);
			String uri = requestMappingAnnotation.getValue();

			if (StringUtils.isBlank(uri))
				throw new IllegalArgumentException(
						"method "
								+ methodDoc.name()
								+ " is missing request mapping annotation's value (uri).");
			// If method uri already exist, add the current httpMethod to the
			// existing method.
			// There can be several httpMethods (GET, POST, DELETE) for each
			// uri.
			DocMethod docMethod = docMethods.get(uri);
			if (docMethod != null)
				docMethod.addHttpMethod(httpMethod);
			else {
				docMethod = new DocMethod(httpMethod);
				docMethod.setUri(uri);
			}

			docMethods.put(docMethod.getUri(), docMethod);
		}
		return docMethods;
	}

	private DocHttpMethod generateHttpMethod(MethodDoc methodDoc,
			String httpMethodName, List<DocAnnotation> annotations) {

		DocHttpMethod httpMethod = new DocHttpMethod(methodDoc.name(), httpMethodName);
		httpMethod.setDescription(methodDoc.commentText());
		httpMethod.setParams(generateParameters(methodDoc));
		httpMethod.setReturnDetails(generateReturnDetails(methodDoc));
		httpMethod.setJsonResponseExample(Utils
				.getJsonResponseExampleAnnotation(annotations));
		httpMethod.setJsonRequesteExample(Utils
				.getJsonRequestExampleAnnotation(annotations));

		DocPossibleResponseStatusesAnnotation possibleResponseStatusesAnnotation = Utils
				.getPossibleResponseStatusesAnnotation(annotations);
		if (possibleResponseStatusesAnnotation != null) {
			Integer[] codes = possibleResponseStatusesAnnotation.getCodes();
			String[] descriptions = possibleResponseStatusesAnnotation.getDescriptions();
			if (codes == null || descriptions == null || codes.length != descriptions.length)
				throw new IllegalArgumentException(
						"In method "
								+ methodDoc.name()
								+ ": wrong attributes for annotation @"
								+ RestDocConstants.POSSIBLE_RESPONSE_STATUSES_DESCRIPTIONS
								+ ".");
			httpMethod.setPossibleResponseStatuses(codes, descriptions);
		}

		if (StringUtils.isBlank(httpMethod.getHttpMethodName()))
			throw new IllegalArgumentException(
					"method "
							+ methodDoc.name()
							+ " is missing request mapping annotation's method (http method).");

		return httpMethod;
	}

	private List<DocParameter> generateParameters(MethodDoc methodDoc) {
		List<DocParameter> paramsList = new LinkedList<DocParameter>();

		for (Parameter parameter : methodDoc.parameters()) {
			DocParameter docParameter = new DocParameter(parameter.name(),
					parameter.type());
			docParameter.setAnnotations(generateAnnotations(parameter
					.annotations()));
			docParameter.setDescription(Utils.getParamTagsComments(methodDoc)
					.get(parameter.name()));

			paramsList.add(docParameter);
		}
		return paramsList;
	}

	private DocReturnDetails generateReturnDetails(MethodDoc methodDoc) {
		DocReturnDetails returnDetails = new DocReturnDetails();
		returnDetails.setReturnType(methodDoc.returnType());
		Tag[] returnTags = methodDoc.tags("return");
		if (returnTags.length > 0) {
			returnDetails.setDescription(returnTags[0].text());
		}
		return returnDetails;
	}

}
