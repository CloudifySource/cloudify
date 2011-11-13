package com.gigaspaces.cloudify.shell.commands;

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

import org.apache.felix.gogo.commands.Command;

import com.j_spaces.kernel.PlatformVersion;

/**
 * @author adam
 * @since 8.0.5
 */
@Command(scope = "cloudify", name = "version", description = "Displays the XAP and cloudify versions")
public class Version extends AbstractGSCommand {


	@Override
	protected Object doExecute() throws Exception {
		
		String platformInfo = PlatformVersion.getOfficialVersion();
		String cloudifyInfo = "Cloudify version 2.0";
		
		String info = platformInfo + System.getProperty("line.separator") + cloudifyInfo;
		
		return info;
	}
}
