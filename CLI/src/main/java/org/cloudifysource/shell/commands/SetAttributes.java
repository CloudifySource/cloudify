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
