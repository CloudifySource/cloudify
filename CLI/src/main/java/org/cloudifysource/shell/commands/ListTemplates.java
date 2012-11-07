package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Command;

/**
 * 
 * List all cloud's templates.
 * 
 * Command syntax: 
 * 			list-templates 
 * 
 * @author yael
 * 
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "list-templates", description = "List all cloud's templates")
public class ListTemplates extends AdminAwareCommand {

	@Override
	protected Object doExecute() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
