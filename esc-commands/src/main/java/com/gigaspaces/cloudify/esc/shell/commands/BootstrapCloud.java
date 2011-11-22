package com.gigaspaces.cloudify.esc.shell.commands;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.gigaspaces.cloudify.esc.shell.installer.CloudGridAgentBootstrapper;
import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.AbstractGSCommand;

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
        
        return getFormattedMessage("cloud_started_successfully", cloudProvider);
        
    }
        
}
