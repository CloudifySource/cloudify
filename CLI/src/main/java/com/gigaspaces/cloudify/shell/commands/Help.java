/**
 *
 */
package com.gigaspaces.cloudify.shell.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.HelpAction;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(name = "help", description = "lists all available commands", scope = "cloudify")
public class Help extends HelpAction {

}
