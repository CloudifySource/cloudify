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

import java.io.File;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * Adds templates to be included in the cloud's templates list. 
 * The templates are being constructed from the (groovy) templates-file.
 * 
 * Required arguments: templates-file - Path to the templates file.
 * 
 * @author yael
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "add-templates", 
description = "Adds templates to the cloud")
public class AddTemplates extends AdminAwareCommand {
	
	@Argument(required = true, name = "add-templates", description = "The templates file path")
	private File templatesFile;
	
	@Override
	protected Object doExecute() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
