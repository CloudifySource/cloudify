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

package org.cloudifysource.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.internal.packaging.Packager;

import com.j_spaces.kernel.Environment;

/**
 * 
 * @author elip
 *
 */
public class RecipePathResolver {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(Packager.class.getName());
	
	private String defaultLocation;
	private File resolved;
	private List<String> pathsLooked = new ArrayList<String>();
	
	public RecipePathResolver(final String defaultLocation) {
		this.defaultLocation = defaultLocation;
	}
	
	public boolean resolve(final File recipeFileOrFolder) {
		
		// if an absolute path was given, just return it
		if (recipeFileOrFolder.isAbsolute()) {
			logger.info("looking for recipe in " + recipeFileOrFolder.getAbsolutePath());
			if (recipeFileOrFolder.exists()) {
				resolved = recipeFileOrFolder;
				return true;
			} else {
				pathsLooked.add(recipeFileOrFolder.getAbsolutePath());
				return false;
			}
		}
		
		// if not, first check in the current directory
		File recipe = null;
		recipe = lookInCurrentDir(recipeFileOrFolder);
		if (recipe != null) {
			resolved = recipe;
			return true;
		}
		recipe = lookInDefaultLocation(recipeFileOrFolder);
		if (recipe != null) {
			resolved = recipe;
			return true;
		}
		return false;
		
	}
	
	private File lookInCurrentDir(final File file) {
		String currentDir = new File(".").getAbsolutePath();
		File fileUnderCurrentDir = new File(currentDir + File.separator + file.getPath());
		logger.info("looking for recipe in " + fileUnderCurrentDir.getAbsolutePath());
		if (fileUnderCurrentDir.exists()) {
			return fileUnderCurrentDir;
		} else {
			pathsLooked.add(fileUnderCurrentDir.getAbsolutePath());
			return null;
		}
	}
	
	private File lookInDefaultLocation(final File file) {
		String homeDir = Environment.getHomeDirectory();
		File fileUnderDefaultLocation = new File(homeDir + defaultLocation + File.separator + file.getPath());
		logger.info("looking for recipe in " + fileUnderDefaultLocation.getAbsolutePath());
		if (fileUnderDefaultLocation.exists()) {
			return fileUnderDefaultLocation;
		} else {
			pathsLooked.add(fileUnderDefaultLocation.getAbsolutePath());
			return null;
		}
	}

	public File getResolved() {
		return resolved;
	}

	public List<String> getPathsLooked() {
		return pathsLooked;
	}

}
