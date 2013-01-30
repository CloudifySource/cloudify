package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;

/**
 * Gets a cloud's template.
 * 
 * Required arguments:
 * 			name - The name of the template to get.
 * 
 * Command syntax:
 * 			get-template name.
 * 
 * @author yael
 * 
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "get-template", description = "Displayes the cloud template details")
public class GetTemplate extends AdminAwareCommand {

	@Argument(required = true, name = "name", description = "The name of the template")
	private String templateName;

	@Override
	protected Object doExecute() throws Exception {

		ComputeTemplate template = adminFacade.getTemplate(templateName);
		return templateName + ":"
		+ CloudifyConstants.NEW_LINE
		+ template.toFormatedString();
	}

}
