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
package com.gigaspaces.cloudify.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;


import java.io.File;
import java.text.MessageFormat;

@Command(scope = "cloudify", name = "install", description = "Installs a service. If you specify a folder path it will be packed and deployed. If you sepcify a service archive, the shell will deploy that file.")
public class Install extends AdminAwareCommand {

    @Argument(index = 0, required = true, name = "service-file", description = "The service recipe folder or archive")
    File serviceFile;

    @Override
    protected Object doExecute() throws Exception {
        if (!serviceFile.exists()) {
            throw new CLIStatusException("service_file_doesnt_exist", serviceFile.getPath());
        }
        File packedFile;

        if (serviceFile.isDirectory()) {
            packedFile = Pack.doPack(serviceFile);
        } else {//serviceFile a file
            packedFile = serviceFile;
        }
        String currentApplicationName = getCurrentApplicationName();
        String serviceName = adminFacade.install(currentApplicationName, packedFile);
        return MessageFormat.format(messages.getString("installed_succesfully"), serviceName, currentApplicationName);

    }


}
