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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi.Color;

/**
 * This command is used to retrieve the last N lines of a specific service log.
 * The tail command can either accept the service name and instance number of a certain instance,
 * or it can get the service name and host address of the instance. The log output can also be 
 * directed to a file using the -file option.
 * @author adaml
 *
 */
@Command(scope = "cloudify", name = "tail", description = "retrieve the last N lines of a specific service log")
public class Tail extends AdminAwareCommand {
	@Argument(index = 0, required = true, description = "The service name who's log to tail")
	private String serviceName;
	
	@Argument(index = 1, required = true, description = "the last N lines to tail")
	private int numLines;
	
	@Option(required = false, description = "the path of the file to save the output to", name = "-file")
	private File file;
	
	@Option(required = false, description = "The service instance's host address", name = "-hostAddress")
	private String hostAddress;
	
	@Option(required = false, description = "the service instance number", name = "-instanceId")
	private Integer instanceId;
	
	@Override
	protected Object doExecute() throws Exception {
		
		String applicationName = getCurrentApplicationName();
		String logTail = "";
		
		boolean twoTailOptionsEntered = (StringUtils.isNotBlank(hostAddress)) && (instanceId != null);
		boolean noTailOptionsEntered = (!StringUtils.isNotBlank(hostAddress)) && (instanceId == null);
		boolean oneTailOtionEntered = ((!twoTailOptionsEntered) && (!noTailOptionsEntered));
		
		if (noTailOptionsEntered) {
			logTail = adminFacade.getTailByServiceName(serviceName, applicationName, numLines);
		}
		
		if (oneTailOtionEntered) {
			if (StringUtils.isNotBlank(hostAddress)) {
				logTail = adminFacade.getTailByHostAddress(serviceName, applicationName, hostAddress, numLines);
			} else {
				logTail = adminFacade.getTailByInstanceId(serviceName, applicationName, instanceId, numLines);
			}
		}
		
		if (twoTailOptionsEntered) {
			throw new CLIStatusException("you_must_set_one_of_tail_option");
		}
		
		if (this.file != null) {
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					throw new CLIStatusException(e, "the_log_could_not_be_saved_to_file", file.getAbsolutePath());
				}
			}
			writeLogToFile(logTail);
			return getFormattedMessage("log_tail_successfully_saved_to_file", file.getAbsolutePath());
		}
		
		String coloredLogTail = getColoredLogTail(logTail);
		
		return coloredLogTail;
	}

	private void writeLogToFile(final String logTail) 
								throws CLIStatusException {
		try {
			FileUtils.writeStringToFile(this.file, logTail);
		} catch (IOException e) {
			throw new CLIStatusException(e, "the_log_could_not_be_saved_to_file", this.file.getAbsolutePath());
		}
		
	}

	private String getColoredLogTail(final String logTail) {

		String result = logTail;
		result = result.replaceAll(" ALL ", ShellUtils.getBoldMessage(" ALL "));
		result = result.replaceAll(" TRACE ", ShellUtils.getBoldMessage(" TRACE "));
		result = result.replaceAll(" WARNING ", ShellUtils.getBoldMessage(" WARNING "));
		result = result.replaceAll(" WARN ", ShellUtils.getBoldMessage(" WARN "));
		result = result.replaceAll(" FINE ", ShellUtils.getBoldMessage(" FINE "));
		result = result.replaceAll(" FINER ", ShellUtils.getBoldMessage(" FINER "));
		result = result.replaceAll(" FINEST ", ShellUtils.getBoldMessage(" FINEST "));
		result = result.replaceAll(" INFO ", ShellUtils.getBoldMessage(" INFO "));
		result = result.replaceAll(" SEVERE ", ShellUtils.getColorMessage(" SEVERE ", Color.RED));
		
		return result;
	}
}

