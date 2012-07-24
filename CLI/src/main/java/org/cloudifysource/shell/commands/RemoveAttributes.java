package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * Created with IntelliJ IDEA.
 * User: uri1803
 * Date: 7/22/12
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
@Command(scope = "cloudify", name = "remove-attributes", description = "Removes attributes from the cloudify controller attribute store")
public class RemoveAttributes extends AbstractAttributesCommand {

    @Argument(required = true, name = "attributes", description = "A list of one or more attributes names to delete. List " +
            "should use the following format: 'attribute 1 name,attribute 2 name\' (make sure to use single quotes (') around " +
            "this argument to make sure all attribute names are escaped properly.")
    protected String attributes = null;

    @Override
    protected Object doExecute() throws Exception {
        String[] attributeNames = parseAttributeNames();
        getRestAdminFacade().deleteAttributes(scope, getCurrentApplicationName(), attributeNames);
        return getFormattedMessage("attributes_removed_successfully");
    }

    private String[] parseAttributeNames() throws CLIException {
        return attributes.split(",");
    }

}
