package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Command;

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

	@Override
	protected Object doExecute() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
