package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.cloud.CloudTemplate;

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
@Command(scope = "cloudify", name = "get-template", description = "Gets a cloud's template")
public class GetTemplate extends AdminAwareCommand {

	@Argument(required = true, name = "name", description = "The name of the template to remove")
	private String templateName;
	
	@Override
	protected Object doExecute() throws Exception {
		
		CloudTemplate template = adminFacade.getTemplate(templateName);
		return template.getFormatedString();
	}

}
