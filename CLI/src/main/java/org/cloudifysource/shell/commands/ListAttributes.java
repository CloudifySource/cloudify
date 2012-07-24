package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.restclient.GSRestClient;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: uri1803
 * Date: 7/22/12
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
@Command(scope = "cloudify", name = "list-attributes", description = "Lists attributes in the cloudify controller attribute store")
public class ListAttributes extends AbstractAttributesCommand {

    @Override
    protected Object doExecute() throws Exception {
        Map<String, String> attributes = getRestAdminFacade().listAttributes(scope, getCurrentApplicationName());
        return GSRestClient.mapToJson(attributes);
    }
}
