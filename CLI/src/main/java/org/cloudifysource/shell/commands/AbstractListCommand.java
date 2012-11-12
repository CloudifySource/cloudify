/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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

import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.shell.ShellUtils;
/**
 * 
 * @author adaml
 * @since 2.3.0
 *
 */
public abstract class AbstractListCommand extends AdminAwareCommand {

	protected static final String NEW_LINE = System.getProperty("line.separator");
	private static final String TAB_CHAR = "\t";

	/**
	 * returns a string representation of the ApplicationDescription object
	 * @param applicationDescription
	 * @return
	 */
	public String getApplicationDescriptionAsString(
			final ApplicationDescription applicationDescription) {
		StringBuilder sb = new StringBuilder();
		sb.append(ShellUtils.getBoldMessage(applicationDescription.getApplicationName())
				+ "  " + applicationDescription.getApplicationState()
				+ TAB_CHAR + "Authorization Groups: " + applicationDescription.getAuthGroups());
		String serviceDescriptionAsString;
		for (ServiceDescription serviceDescription : applicationDescription.getServicesDescription()) {
			serviceDescriptionAsString = getServiceDescriptionAsString(serviceDescription);
			sb.append(NEW_LINE);
			sb.append(TAB_CHAR + serviceDescriptionAsString);
		}
		return sb.toString();
	}

	private String getServiceDescriptionAsString(
			final ServiceDescription serviceDescription) {
		StringBuilder sb = new StringBuilder();
		String absolutePUName = ServiceUtils.getAbsolutePUName(serviceDescription.getApplicationName(), 
				serviceDescription.getServiceName());
		sb.append(ShellUtils.getBoldMessage(absolutePUName)
				+ "  " + serviceDescription.getServiceState());
		sb.append(" (" + serviceDescription.getInstanceCount() + "/" + serviceDescription.getPlannedInstances() + ")" );
		
		return sb.toString();
		
	}

}
