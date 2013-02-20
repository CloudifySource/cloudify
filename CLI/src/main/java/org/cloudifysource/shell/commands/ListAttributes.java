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

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.restclient.GSRestClient;

import java.util.Map;

/**
 * @author uri
 * @since 2.2.0
 * 
 *        Lists attributes in the cloudify controller attribute store.
 * 
 *        Command syntax: list-attributes
 */
@Command(scope = "cloudify", name = "list-attributes",
		description = "Lists attributes in the cloudify controller attribute store")
public class ListAttributes extends AbstractAttributesCommand {

    @Override
    protected Object doExecute() throws Exception {
        Map<String, String> attributes = getRestAdminFacade().listAttributes(scope, getCurrentApplicationName());
        return GSRestClient.mapToJson(attributes);
    }
}
