package com.gigaspaces.cloudify.shell.commands;

import java.text.MessageFormat;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(scope = "cloudify", name = "remove-instance", description = "removes a service instance")
public class RemoveInstance extends AdminAwareCommand {

    @Argument(index = 0, name = "service-name", required = true, description = "The name of the service. Use tab to list the currently running services")
    private String serviceName;

    @Argument(index = 1, name = "instance-id", required = true, description = "The IDs of the service instance to remove. Press tab to see the complete list of IDs")
    private int instanceId;

/*    @CompleterValues(index = 0)
    public Collection<String> getServiceList() {
        try {
            return adminFacade.getServicesList(getCurrentApplicationName());
        } catch (Exception e) {
            logger.warning("Could not get service list: " + e.getMessage());
            return null;
        }
    }

    @CompleterValues(index = 1)
    public Collection<String> getServiceInstanceList() {
        if (serviceName != null) {
            try {
                Map<String, Object> instanceIdToHost = adminFacade.getInstanceList(getCurrentApplicationName(), serviceName);
                return instanceIdToHost.keySet();
            } catch (Exception e) {
                logger.warning("Could not fetch instance list for service " + serviceName + ":" + e.getMessage());
                return null;
            }
        }
        return null;
    }
*/
    @Override
    protected Object doExecute() throws Exception {
        adminFacade.removeInstance(getCurrentApplicationName(), serviceName, instanceId);
        return MessageFormat.format(messages.getString("instance_removed_successfully"), instanceId);
    }


}
