package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.GetTemplateResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.rest.RestAdminFacade;

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
public class GetTemplate extends AdminAwareCommand implements NewRestClientCommand {

	@Argument(required = true, name = "name", description = "The name of the template")
	private String templateName;

	@Override
	protected Object doExecute() throws Exception {

		ComputeTemplate template = adminFacade.getTemplate(templateName);
		return templateName + ":"
		+ CloudifyConstants.NEW_LINE
		+ template.toFormatedString();
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {
		final RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();
		GetTemplateResponse response = newRestClient.getTemplate(templateName);
		return templateName + ":"
				+ CloudifyConstants.NEW_LINE
				+ response.getTemplate().toFormatedString();
	}

}
