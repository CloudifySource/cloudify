package org.openspaces.cloud.shell.commands;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.openspaces.cloud.shell.installer.CloudGridAgentBootstrapper;
import org.openspaces.shell.AdminFacade;
import org.openspaces.shell.Constants;
import org.openspaces.shell.ShellUtils;
import org.openspaces.shell.commands.AbstractGSCommand;

@Command(scope = "cloudify", name = "bootstrap-cloud", description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on the provided cloud.")
public class BootstrapCloud extends AbstractGSCommand {

    @Argument(required = true, name = "provider", description = "the cloud prodiver to use")
    String cloudProvider;
    
    @Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
    int timeoutInMinutes = 60;
    
    @Override
    protected Object doExecute() throws Exception {

        CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
        
        // TODO use DSL
        String pathSeparator = System.getProperty("file.separator");
        File providerDirectory = new File(ShellUtils.getCliDirectory(), 
                "plugins" + pathSeparator + "esc"+ pathSeparator + cloudProvider);
        
        installer.setProviderDirectory(providerDirectory);
        installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
        installer.setProgressInSeconds(10);
        installer.setVerbose(verbose);
        
        installer.boostrapCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);
        
        return "Cloud started succesfully. Use the 'teardown-cloud " + cloudProvider + "' command to terminate all machines.";
        
    }
        
}
