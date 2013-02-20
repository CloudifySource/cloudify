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

package org.cloudifysource.dsl.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.j_spaces.kernel.Environment;

/**
 * This class is used for any logic that we may need for resolving file paths passed to us by the user in the cli.
 * @author elip
 * 
 * 
 * <br><br>	
 * resolves a given file using the following strategy : <br>
 * <h1>	1. if the file is absolute, check for existence.</h1> <br>
 * <h1>	2. if not, check for the relative path under the current directory.</h1> <br>
 * <h1>	3. otherwise, check for the relative path under the default location.</h1> <br>
 * 
 * 
 *
 */
public class RecipePathResolver {
	
	private static final String DEFAULT_SERVICES_PATH = "/recipes/services";
	private static final String DEFAULT_APPS_PATH = "/recipes/apps";
	private static final String DEFAULT_CLOUDS_PATH = "/clouds";
	
	private File currentDir = new File(".");
	
	private File resolved;
	private List<String> pathsLooked = new ArrayList<String>();
	
	public void setCurrentDirectory(final File currentDir) {
		this.currentDir = currentDir;
	}
	
	/**
	 * Resolves a location of a service recipe. default location used is <cloudify-home>/recipes/services.
	 * @param recipeFileOrFolder .
	 * @return true if the file was found during the location lookup, false otherwise.
	 */
	public boolean resolveService(final File recipeFileOrFolder) {
		return resolve(recipeFileOrFolder, DEFAULT_SERVICES_PATH);
		
	}
	
	/**
	 * Resolves a location of an application recipe. default location used is <cloudify-home>/recipes/apps.
	 * @param recipeFileOrFolder .
	 * @return true if the file was found during the location lookup, false otherwise.
	 */
	public boolean resolveApplication(final File recipeFileOrFolder) {
		return resolve(recipeFileOrFolder, DEFAULT_APPS_PATH);
	}
	
	/**
	 * Resolves a location of a cloud driver configuration files. default location used is <cloudify-home>/recipes/apps.
	 * @param cloudRecipeFileOrFolder .
	 * @return true if the file was found during the location lookup, false otherwise.
	 */
	public boolean resolveCloud(final File cloudRecipeFileOrFolder) {
		return resolve(cloudRecipeFileOrFolder, DEFAULT_CLOUDS_PATH);
	}
	
	public File getResolved() {
		return resolved;
	}

	public List<String> getPathsLooked() {
		return pathsLooked;
	}

	
	private boolean resolve(final File recipeFileOrFolder, final String defaultLocation) {
		
		// if an absolute path was given, just return it
		if (recipeFileOrFolder.isAbsolute()) {
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
		recipe = lookInDefaultLocation(recipeFileOrFolder, defaultLocation);
		if (recipe != null) {
			resolved = recipe;
			return true;
		}
		return false;	
	}
	
	private File lookInCurrentDir(final File file) {
		File fileUnderCurrentDir = new File(currentDir + File.separator + file.getPath());
		if (fileUnderCurrentDir.exists()) {
			return fileUnderCurrentDir;
		} else {
			pathsLooked.add(fileUnderCurrentDir.getAbsolutePath());
			return null;
		}
	}
	
	private File lookInDefaultLocation(final File file, final String defaultLocation) {
		String homeDir = Environment.getHomeDirectory();
		File fileUnderDefaultLocation = new File(homeDir + defaultLocation + File.separator + file.getPath());
		if (fileUnderDefaultLocation.exists()) {
			return fileUnderDefaultLocation;
		} else {
			pathsLooked.add(fileUnderDefaultLocation.getAbsolutePath());
			return null;
		}
	}
}
