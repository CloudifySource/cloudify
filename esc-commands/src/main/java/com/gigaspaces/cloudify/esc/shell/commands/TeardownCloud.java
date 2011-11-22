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

@Command(scope = "cloudify", name = "teardown-cloud", description = "Terminates management machines.")
public class TeardownCloud extends AbstractGSCommand {

    @Argument(required = true, name = "provider", description = "the cloud prodiver to use")
    String cloudProvider;
    
    @Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
    int timeoutInMinutes = 60;
    
    @Option(required = false, name = "-force", description = "Should management machine be shutdown if other applications are installed")
    boolean force = false;
    
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
        installer.setForce(force);
        
        installer.teardownCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);
        
        return "Cloud terminated succesfully.";
        
    }
    
}
