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
package org.cloudifysource.dsl;

import java.io.Serializable;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/*************
 * Domain POJO of the service lifecycle, part of the Service Recipe declaration. Non-null Elements in the lifecycle POJO
 * indicate actions that should be executed when the service reaches a certain lifecycle phase. Executable entries can
 * take one of several forms: <br>
 * 1. String - indicates a command line to execute (command lines may be modified according
 * to USM rules) 
 * 2. Closure - code that executes in-process 3. Map<String, String> - Where keys are Java regular
 * expressions and values are command lines (as in 1). The entry to be executed is the first one where 
 * the key regex matches the operating system name of the host running the service. 
 * Common keys include 'Win.*', 'Linux.*', etc.
 * 
 * See the documentation for examples.
 * 
 * @author barakme.
 * @since 2.0.0
 * 
 */
@CloudifyDSLEntity(name = "lifecycle", clazz = ServiceLifecycle.class, allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class ServiceLifecycle implements Serializable {

	private static final int DEFAULT_START_DETECTION_SECONDS = 90;

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	private Object init;

	private Object preInstall;
	private Object install;
	private Object postInstall;

	private Object preStart;
	private Object start;
	private Object postStart;

	private Object preStop;
	private Object stop;
	private Object postStop;

	private Object shutdown;

	private Object preServiceStart;
	private Object preServiceStop;

	private Object startDetection;
	private Object monitors;
	private Object details;

	private Object locator;
	
	private int startDetectionTimeoutSecs = DEFAULT_START_DETECTION_SECONDS;

	public int getStartDetectionTimeoutSecs() {
		return startDetectionTimeoutSecs;
	}

	public void setStartDetectionTimeoutSecs(final int startDetectionTimeoutSecs) {
		this.startDetectionTimeoutSecs = startDetectionTimeoutSecs;
	}

	public int getStartDetectionIntervalSecs() {
		return startDetectionIntervalSecs;
	}

	public void setStartDetectionIntervalSecs(final int startDetectionIntervalSecs) {
		this.startDetectionIntervalSecs = startDetectionIntervalSecs;
	}

	private int startDetectionIntervalSecs = 1;

	private Object stopDetection;

	public void setStopDetection(final Object stopDetection) {
		this.stopDetection = stopDetection;
	}

	/********
	 * Default Constructor.
	 */
	public ServiceLifecycle() {

	}

	public Object getInit() {
		return init;
	}

	public void setInit(final Object init) {
		this.init = init;
	}

	public Object getPreInstall() {
		return preInstall;
	}

	public void setPreInstall(final Object preInstall) {
		this.preInstall = preInstall;
	}

	public Object getInstall() {
		return install;
	}

	public void setInstall(final Object install) {
		this.install = install;
	}

	public Object getPostInstall() {
		return postInstall;
	}

	public void setPostInstall(final Object postInstall) {
		this.postInstall = postInstall;
	}

	public Object getPreStart() {
		return preStart;
	}

	public void setPreStart(final Object preStart) {
		this.preStart = preStart;
	}

	public Object getStart() {
		return start;
	}

	public void setStart(final Object start) {
		this.start = start;
	}

	public Object getPostStart() {
		return postStart;
	}

	public void setPostStart(final Object postStart) {
		this.postStart = postStart;
	}

	public Object getPreStop() {
		return preStop;
	}

	public void setPreStop(final Object preStop) {
		this.preStop = preStop;
	}

	public Object getStop() {
		return stop;
	}

	public void setStop(final Object stop) {
		this.stop = stop;
	}

	public Object getPostStop() {
		return postStop;
	}

	public void setPostStop(final Object postStop) {
		this.postStop = postStop;
	}

	public Object getShutdown() {
		return shutdown;
	}

	public void setShutdown(final Object shutdown) {
		this.shutdown = shutdown;
	}

	public void setPreServiceStart(final Object preServiceStart) {
		this.preServiceStart = preServiceStart;
	}

	public Object getPreServiceStart() {
		return this.preServiceStart;
	}

	public void setPreServiceStop(final Object preServiceStop) {
		this.preServiceStop = preServiceStop;
	}

	public Object getPreServiceStop() {
		return this.preServiceStop;
	}

	public void setStartDetection(final Object startDetection) {
		this.startDetection = startDetection;
	}

	public Object getStartDetection() {
		return this.startDetection;
	}

	public Object getStopDetection() {
		return this.stopDetection;
	}

	public void setMonitors(final Object monitors) {
		this.monitors = monitors;
	}

	public Object getMonitors() {
		return this.monitors;
	}

	public void setDetails(final Object details) {
		this.details = details;
	}

	public Object getDetails() {
		return details;
	}

	public Object getLocator() {
		return locator;
	}

	public void setLocator(final Object locator) {
		this.locator = locator;
	}
}
