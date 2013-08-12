/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.j_spaces.kernel.Environment;

/**
 * Utility Class for Environment issues. (File location, variables,..)
 * 
 * @author elip
 * 
 */
public final class EnvironmentUtils {

	/**
	 * To prevent initialization.
	 */
	private EnvironmentUtils() {

	}

	/**
	 * Retrieve webui war file path from installation directory.
	 * 
	 * @return - File path to the webui war.
	 */
	public static String findWebuiWar() {
		final File webuiDir = new File(Environment.getHomeDirectory()
				+ File.separator + "tools" + File.separator + "gs-webui");
		if (!webuiDir.exists()) {
			throw new IllegalStateException("Missing web-ui folder: " + webuiDir.getAbsolutePath());
		}

		Set<File> filterSet = FileFilterUtils.filterSet(new WildcardFileFilter("gs-webui*.war"), webuiDir.listFiles());
		if (filterSet.size() > 1) {
			throw new IllegalStateException("Cannot have two war files under gs-webui folder: "
					+ webuiDir.getAbsolutePath());
		} else if (filterSet.isEmpty()) {
			throw new IllegalStateException("Could not find war file in gs-webui folder: "
					+ webuiDir.getAbsolutePath());
		} else {
			return filterSet.iterator().next().getAbsolutePath();
		}

	}

	/**
	 * Retrieve rest war file path from installation directory.
	 * 
	 * @return - File path to the rest war.
	 */
	public static String findRestWar() {
		final File webuiDir = new File(Environment.getHomeDirectory()
				+ File.separator + "tools" + File.separator + "rest");
		Set<File> filterSet = FileFilterUtils.filterSet(new WildcardFileFilter("rest*.war"), webuiDir.listFiles());
		if (filterSet.size() > 1) {
			throw new IllegalStateException("Cannot have two war files under rest folder");
		} else {
			return filterSet.iterator().next().getAbsolutePath();
		}

	}

}
