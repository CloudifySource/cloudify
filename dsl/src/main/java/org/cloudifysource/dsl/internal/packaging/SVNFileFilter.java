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
package org.cloudifysource.dsl.internal.packaging;

import java.io.File;
import java.io.FileFilter;

/************
 * File filter to remove SVN related files when iterating over files - useful when creating zip files
 * from directories so un-required svn related files will not be packaged.
 * @author barakme
 *
 */
public final class SVNFileFilter implements FileFilter {

	private static SVNFileFilter instance = new SVNFileFilter();

	private SVNFileFilter() {
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(final File pathname) {
		return !pathname.getName().equals(".svn");
	}

	public static FileFilter getFilter() {
		return instance;
	}

}