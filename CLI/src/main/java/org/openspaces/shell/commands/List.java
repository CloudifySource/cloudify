/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openspaces.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.openspaces.shell.ComponentType;

import java.text.MessageFormat;
import java.util.Collection;

import static org.openspaces.shell.ShellUtils.componentTypeFromLowerCaseComponentName;

/**
 * @author uri
 */

@Command(scope = "cloudify", name = "list", description = "Lists all running component of a certain type")
public class List extends AdminAwareCommand {

    @Argument(index = 0, name = "component-type", required = true, description = "The component type to list. Press tab to see available options")
    private String componentType;

    @CompleterValues(index = 0)
    public Collection<String> getCompleterValues() {
//        return ShellUtils.getComponentTypesAsLowerCaseStringCollection();
        return null;
    }

    @Override
    protected Object doExecute() throws Exception {
        ComponentType componentTypeEnum = componentTypeFromLowerCaseComponentName(componentType);
        if (componentType == null) {
            throw new IllegalArgumentException(MessageFormat.format(messages.getString("unknown_service_type"), componentType));
        }
        switch (componentTypeEnum) {
            case SERVICE:
                return adminFacade.getServicesList(getCurrentApplicationName());
            case APPLICATION:
                return adminFacade.getApplicationsList();
            default:
                logger.warning("Unhandled component type");
                return null;
        }
    }

}
