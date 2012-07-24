package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.restclient.GSRestClient;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: uri1803
 * Date: 7/22/12
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
@Command(scope = "cloudify", name = "set-attributes", description = "Sets attributes in the cloudify controller attribute store")
public class SetAttributes extends AbstractAttributesCommand {

    @Argument(required = true, name = "attributes", description = "A list of one or more attributes to store. List " +
            "should use a valid JSON format, e.g. '{\"attribute 1 name\":\"attribute 1 value\",\"attribute 2 name\":\"attribute 2 value\"}' " +
            "(make sure to use single quotes (') around this argument to make sure all JSON attributes are escaped properly.")
    protected String attributes = null;

    @Override
    protected Object doExecute() throws Exception {
        Map attributes = parseAttributes();
        getRestAdminFacade().updateAttributes(scope, getCurrentApplicationName(), attributes);
        return getFormattedMessage("attributes_updated_successfully");
    }

    private Map<String, Object> parseAttributes() throws CLIException {
        try {
            return GSRestClient.jsonToMap(attributes);
        } catch (IOException e) {
            throw new CLIStatusException("illegal_attribute_format", attributes);
        }
    }

}
