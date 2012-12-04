package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author noak
 * @since 2.3.0
 * 
 *        Logs-in to the REST server with the given credentials
 * 
 *        Command syntax: login username password
 */
@Command(scope = "cloudify", name = "login", description = "reconnects to the admin REST server")
public class Login extends AdminAwareCommand {

	@Argument(required = true, name = "user", description = "The username for a secure connection to the rest "
			+ "server", index=0)
	private String username;
	
	@Argument(required = true, name = "password", description = "The password for a secure connection to the rest "
			+ " server", index=1)
	private String password;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		adminFacade.reconnect(username, password);
		return getFormattedMessage("logged_in_successfully", Color.GREEN);
	}
}
