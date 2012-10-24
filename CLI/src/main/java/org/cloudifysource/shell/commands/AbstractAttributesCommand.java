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
 *******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Option;

/**
 * 
 * @author uri
 * @since 2.2.0
 *
 */
public abstract class AbstractAttributesCommand extends AdminAwareCommand {


    @Option(required = false, name = "-scope", 
    		description = "The attributes scope. Can be \"global\", \"application\"," 
    		+ " which will apply to the current application, \"service:<service name>\", which will apply to the " 
    		+ " service with the given name of the current application, or \"service:<service name>:<instance id>\", " 
    		+ " which will apply to the instance with the give ID of the given service of the current application."
    		+ " You can also specify \"service:<service name>:all-instances\" to apply to the instance-level" 
            + " attributes of all instances of a specific service")
    protected String scope = "global";
}
