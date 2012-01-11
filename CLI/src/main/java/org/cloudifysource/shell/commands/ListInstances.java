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
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.cloudifysource.shell.Constants;


import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;


/**
 * @author uri
 */

@Command(scope = "cloudify", name = "list-instances", description = "Lists all instances of a certain service")
public class ListInstances extends AdminAwareCommand {

    @Argument(index = 0, name = "service-name", required = true, description = "The service name")
    private String serviceName;

    @CompleterValues(index = 0)
    public Collection<String> getCompleterValues() throws Exception {
        return adminFacade.getServicesList(getCurrentApplicationName());
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<String, Object> instanceIdToHostMap = adminFacade.getInstanceList((String) session.get(Constants.ACTIVE_APP), serviceName);
        if (instanceIdToHostMap.isEmpty()) {
            return MessageFormat.format(messages.getString("no_instances_found"), serviceName);
        }
        StringBuilder builder = new StringBuilder("Instance\t\tHost\n");
        for (Map.Entry<String, Object> entry : instanceIdToHostMap.entrySet()) {
            builder.append("instance #").append(entry.getKey()).append("\t\t").append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

}
