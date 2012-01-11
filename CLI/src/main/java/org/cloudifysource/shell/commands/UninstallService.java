package org.cloudifysource.shell.commands;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ConditionLatch.Predicate;
import org.cloudifysource.shell.rest.RestAdminFacade;


/**
 * @author rafi
 * @since 8.0.3
 */
@Command(scope = "grid", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

    private static final String DEFAULT_APPLICATION_NAME = "default";

	private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for service to uninstall";
	
	private String lastMessage;
    
    @Argument(index = 0, required = true, name = "service-name")
    String serviceName;

    @CompleterValues(index = 0)
    public Collection<String> getServiceList() {
        try {
        	return getRestAdminFacade().getServicesList(DEFAULT_APPLICATION_NAME);
        } catch (Exception e) {
        	return new ArrayList<String>();
        }
    }


	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. Defaults to 5 minutes.")
    int timeoutInMinutes=5;
    
    @Option(required = false, name = "-progress", description = "The polling time interval in seconds, used for checking if the operation is done. Defaults to 5 seconds. Use together with the -timeout option")
    int progressInSeconds=5;
    
    @Override
    protected Object doExecute() throws Exception {
    	
    	if (!askUninstallConfirmationQuestion()){
    		return getFormattedMessage("uninstall_aborted");
    	}
    	
        final Set<String> containerIdsOfService = ((RestAdminFacade)adminFacade).getGridServiceContainerUidsForService(getCurrentApplicationName(), serviceName);
        if (verbose) {
            logger.info("Found containers: " + containerIdsOfService);
        }
        if (timeoutInMinutes > 0){
        	printStatusMessage(containerIdsOfService.size(), containerIdsOfService.size(), containerIdsOfService);
        }
        adminFacade.undeploy(getCurrentApplicationName(), serviceName);
        if (timeoutInMinutes > 0) {
            createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {
                @Override
                public boolean isDone() throws CLIException {
                    Set<String> allContainerIds = ((RestAdminFacade)adminFacade).getGridServiceContainerUids();
                    Set<String> remainingContainersForService = new HashSet<String>(containerIdsOfService);
                    remainingContainersForService.retainAll(allContainerIds);

                    boolean isDone = remainingContainersForService.isEmpty();
                    
                    if (!isDone) {
                    	printStatusMessage(remainingContainersForService.size(), containerIdsOfService.size(), remainingContainersForService);
                    }
                    //TODO: container has already been removed by uninstall.
                    //adminFacade.printEventLogs(getCurrentApplicationName(), serviceName);
                    return isDone;
                }
            });         
        }          
        return getFormattedMessage("undeployed_successfully", serviceName);
    }
    
    private void printStatusMessage(int remainingServiceContainers, int allServiceContainers, Set<String> remainingContainerIDs){
    	String message = "Waiting for all service instances to uninstall. " +
        "Currently " + remainingServiceContainers + " instances of " + allServiceContainers + " are still running.";
    	
    	if (!StringUtils.equals(message, lastMessage)){
    		logger.info(message + (verbose ? " " + remainingContainerIDs : ""));
    		this.lastMessage = message;
    	}
    	
    }
    
	//returns true if the answer to the question was 'Yes'.
	private boolean askUninstallConfirmationQuestion() throws IOException {
		
		//we skip question if the shell is running a script.
		if ((Boolean)session.get(Constants.INTERACTIVE_MODE)){
			String confirmationQuestion = getFormattedMessage("service_uninstall_confirmation", serviceName);
			System.out.print(confirmationQuestion);
			System.out.flush();
			PropertiesReader pr = new PropertiesReader(new InputStreamReader(System.in));
			String answer = pr.readProperty();
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(answer) ? true : false;

		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}

    private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
        return 
            new ConditionLatch()
            .timeout(timeout,timeunit)
            .pollingInterval(progressInSeconds, TimeUnit.SECONDS)
            .timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
            .verbose(verbose);
    }

}
