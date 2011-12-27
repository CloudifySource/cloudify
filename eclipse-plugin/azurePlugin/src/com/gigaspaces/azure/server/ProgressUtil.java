/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package com.gigaspaces.azure.server;

import org.eclipse.core.runtime.*;
/**
 * Progress Monitor utility.
 */
public class ProgressUtil {
	/**
	 * ProgressUtil constructor comment.
	 */
	private ProgressUtil() {
		super();
	}

	/**
	 * Return a valid progress monitor.
	 *
	 * @param monitor org.eclipse.core.runtime.IProgressMonitor
	 * @return org.eclipse.core.runtime.IProgressMonitor
	 */
	public static IProgressMonitor getMonitorFor(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();
		return monitor;
	}

	/**
	 * Return a sub-progress monitor with the given amount on the
	 * current progress monitor.
	 *
	 * @param monitor org.eclipse.core.runtime.IProgressMonitor
	 * @param ticks int
	 * @return org.eclipse.core.runtime.IProgressMonitor
	 */
	public static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks);
	}

	/**
	 * Return a sub-progress monitor with the given amount on the
	 * current progress monitor.
	 *
	 * @param monitor org.eclipse.core.runtime.IProgressMonitor
	 * @param ticks a number of ticks
	 * @param style a style
	 * @return org.eclipse.core.runtime.IProgressMonitor
	 */
	public static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks, int style) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks, style);
	}
}