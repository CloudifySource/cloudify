package org.cloudifysource.dsl.internal.packaging;

import java.io.File;
import java.io.FileFilter;

public class SVNFileFilter implements FileFilter {

	private static SVNFileFilter instance = new SVNFileFilter();
	
	private SVNFileFilter(){};
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File pathname) {
		return !pathname.getName().equals(".svn");
	}

	public static FileFilter getFilter() {
		return instance;
	}

}