package org.openspaces.shell.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.openspaces.shell.ConditionLatch;
import org.openspaces.shell.ConditionLatch.Predicate;
import org.openspaces.shell.rest.RestAdminFacade;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(scope = "grid", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

    private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for service to uninstall";
    
    @Argument(index = 0, required = true, name = "service-name")
    String serviceName;

    @CompleterValues(index = 1)
    public Collection<String> getServiceList() {
        try {
            return adminFacade.getServicesList(serviceName);
        } catch (Exception e) {
            logger.warning("Could not get list of services: " + e.getMessage());
            return null;
        }
    }

    @Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. Defaults to 5 minutes.")
    int timeoutInMinutes=5;
    
    @Option(required = false, name = "-progress", description = "The polling time interval in minutes, used for checking if the operation is done. Defaults to 1 minute. Use together with the -timeout option")
    int progressInMinutes=1;
    
    @Override
    protected Object doExecute() throws Exception {
        
        final Set<String> containerIdsOfService = ((RestAdminFacade)adminFacade).getGridServiceContainerUidsForService(getCurrentApplicationName(), serviceName);
        if (verbose) {
            logger.info("Found containers: " + containerIdsOfService);
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
                        logger.info(
                                "Waiting for all service instances to uninstall. "+
                                "Currently " +remainingContainersForService.size() + " instances are still running." +
                                (verbose ? " " + remainingContainersForService : ""));
                        
                    }
                    //TODO: container has already been removed by uninstall.
                    //adminFacade.printEventLogs(getCurrentApplicationName(), serviceName);
                    return isDone;
                }
            });         
        }          
        return getFormattedMessage("undeployed_successfully", serviceName);
    }

    private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
        return 
            new ConditionLatch()
            .timeout(timeout,timeunit)
            .pollingInterval(progressInMinutes, TimeUnit.MINUTES)
            .timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
            .verbose(verbose);
    }

}
