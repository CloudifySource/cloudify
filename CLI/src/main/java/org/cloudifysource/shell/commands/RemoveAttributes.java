/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * @author uri
 * @since 2.2.0
 * 
 *        Removes attributes from the cloudify controller attribute store.
 * 
 *        Command syntax: remove-attributes 'att1,att2'
 */
@Command(scope = "cloudify", name = "remove-attributes", description = "Removes attributes from the cloudify controller attribute store")
public class RemoveAttributes extends AbstractAttributesCommand {

	@Argument(required = true, name = "attributes", description = "A list of one or more attributes names to delete. List " +
            "should use the following format: 'att1,att2' (make sure to use single quotes (') around " +
            "this argument to make sure all attribute names are escaped properly.")
    protected String attributes;

    @Override
    protected Object doExecute() throws Exception {
        String[] attributeNames = parseAttributeNames();
        getRestAdminFacade().deleteAttributes(scope, getCurrentApplicationName(), attributeNames);
        return getFormattedMessage("attributes_removed_successfully");
    }

    private String[] parseAttributeNames(){
        return attributes.split(",");
    }

}
