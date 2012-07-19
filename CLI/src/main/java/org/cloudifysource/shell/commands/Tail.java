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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi.Color;

/**
 * This command is used to retrieve the last N lines of a specific service log.
 * The tail command can either accept the service name and instance number of a certain instance,
 * or it can get the service name and host address of the instance.
 * @author adaml
 *
 */
@Command(scope = "cloudify", name = "tail", description = "connects to the target admin REST server")
public class Tail extends AdminAwareCommand {
	@Argument(index = 0, required = true, description = "The service name who's log to tail")
	private String serviceName;
	
	@Argument(index = 1, required = true, description = "the last N lines to tail")
	private int numLines;
	
	@Option(required = false, description = "The service instance's host address", name = "-hostAddress")
	private String hostAddress;
	
	@Option(required = false, description = "the service instance number", name = "-instanceId")
	private int instanceId;
	
	@Override
	protected Object doExecute() throws Exception {
		
		String applicationName = getCurrentApplicationName();
		String logTail;
		
		if (StringUtils.isNotBlank(hostAddress)) {
			logTail = adminFacade.getTailByHostAddress(serviceName, applicationName, hostAddress, numLines);
		} else {
			logTail = adminFacade.getTailByInstanceId(serviceName, applicationName, instanceId, numLines);
		}
		
		String coloredLogTail = colorLogTail(logTail);
		
		return coloredLogTail;
		
	}
	
	private String colorLogTail(final String logTail) {

		String result = logTail;
		result = result.replaceAll(" WARNING ", ShellUtils.getBoldMessage(" WARNING "));
		result = result.replaceAll(" FINE ", ShellUtils.getBoldMessage(" FINE "));
		result = result.replaceAll(" FINER ", ShellUtils.getBoldMessage(" FINER "));
		result = result.replaceAll(" FINEST ", ShellUtils.getBoldMessage(" FINEST "));
		result = result.replaceAll(" INFO ", ShellUtils.getBoldMessage(" INFO "));
		result = result.replaceAll(" SEVERE ", ShellUtils.getColorMessage(" SEVERE ", Color.RED));
		
		return result;
	}
}

