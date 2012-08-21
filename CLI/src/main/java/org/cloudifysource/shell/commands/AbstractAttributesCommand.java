package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Option;

/**
 * Created with IntelliJ IDEA.
 * User: uri1803
 * Date: 7/22/12
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractAttributesCommand extends AdminAwareCommand {


    @Option(required = false, name = "-scope", 
    		description = "The attributes scope. Can be \"global\", \"application\"," 
    		+ " which will apply to the current application, \"service:<service name>\", which will apply to the " 
    		+ " service with the given name of the current application, or \"service:<service name>:<instance id>\", " 
    		+ " which will apply to the instance with the give ID of the given service of the current application."
    		+ " You can also specify \"service:<service name>:all-instances\" to apply to the instance-level" 
            + " attributes of all instances of a specific service")
    protected String scope = "global";
}
