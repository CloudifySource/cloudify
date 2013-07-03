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
package org.cloudifysource.dsl;

import java.io.Serializable;

import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/*************
 * Domain POJO of the service lifecycle, part of the Service Recipe declaration. Non-null Elements in the lifecycle POJO
 * indicate actions that should be executed when the service reaches a certain lifecycle phase. Executable entries can
 * take one of several forms: <br>
 * 1. String - indicates a command line to execute (command lines may be modified according to USM rules) 2. Closure -
 * code that executes in-process 3. Map<String, String> - Where keys are Java regular expressions and values are command
 * lines (as in 1). The entry to be executed is the first one where the key regex matches the operating system name of
 * the host running the service. Common keys include 'Win.*', 'Linux.*', etc.
 *
 * See the documentation for examples.
 *
 * @author barakme.
 * @since 2.0.0
 *
 */
@CloudifyDSLEntity(name = "lifecycle", clazz = ServiceLifecycle.class, allowInternalNode = true, allowRootNode = true,
		parent = "service")
public class ServiceLifecycle implements Serializable {

	private static final int DEFAULT_START_DETECTION_INTERVAL_SECONDS = 5;

	private static final int DEFAULT_START_DETECTION_SECONDS = 90;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private ExecutableDSLEntry init;

	private ExecutableDSLEntry preInstall;
	private ExecutableDSLEntry install;
	private ExecutableDSLEntry postInstall;

	private ExecutableDSLEntry preStart;
	private ExecutableDSLEntry start;
	private ExecutableDSLEntry postStart;

	private ExecutableDSLEntry preStop;
	private ExecutableDSLEntry stop;
	private ExecutableDSLEntry postStop;

	private ExecutableDSLEntry shutdown;

	private ExecutableDSLEntry preServiceStart;
	private ExecutableDSLEntry preServiceStop;

	private ExecutableDSLEntry startDetection;
	private Object monitors;
	private Object details;

	private ExecutableDSLEntry locator;

	private ExecutableDSLEntry stopDetection;

	private int startDetectionTimeoutSecs = DEFAULT_START_DETECTION_SECONDS;
	private int startDetectionIntervalSecs = DEFAULT_START_DETECTION_INTERVAL_SECONDS;

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

	/********
	 * Default Constructor.
	 */
	public ServiceLifecycle() {

	}

	public ExecutableDSLEntry getInit() {
		return init;
	}

	public void setInit(final ExecutableDSLEntry init) {
		this.init = init;
	}

	public ExecutableDSLEntry getPreInstall() {
		return preInstall;
	}

	public void setPreInstall(final ExecutableDSLEntry preInstall) {
		this.preInstall = preInstall;
	}

	public ExecutableDSLEntry getInstall() {
		return install;
	}

	public void setInstall(final ExecutableDSLEntry install) {
		this.install = install;
	}

	public ExecutableDSLEntry getPostInstall() {
		return postInstall;
	}

	public void setPostInstall(final ExecutableDSLEntry postInstall) {
		this.postInstall = postInstall;
	}

	public ExecutableDSLEntry getPreStart() {
		return preStart;
	}

	public void setPreStart(final ExecutableDSLEntry preStart) {
		this.preStart = preStart;
	}

	public ExecutableDSLEntry getStart() {
		return start;
	}

	public void setStart(final ExecutableDSLEntry start) {
		this.start = start;
	}

	public ExecutableDSLEntry getPostStart() {
		return postStart;
	}

	public void setPostStart(final ExecutableDSLEntry postStart) {
		this.postStart = postStart;
	}

	public ExecutableDSLEntry getPreStop() {
		return preStop;
	}

	public void setPreStop(final ExecutableDSLEntry preStop) {
		this.preStop = preStop;
	}

	public ExecutableDSLEntry getStop() {
		return stop;
	}

	public void setStop(final ExecutableDSLEntry stop) {
		this.stop = stop;
	}

	public ExecutableDSLEntry getPostStop() {
		return postStop;
	}

	public void setPostStop(final ExecutableDSLEntry postStop) {
		this.postStop = postStop;
	}

	public ExecutableDSLEntry getShutdown() {
		return shutdown;
	}

	public void setShutdown(final ExecutableDSLEntry shutdown) {
		this.shutdown = shutdown;
	}

	public ExecutableDSLEntry getPreServiceStart() {
		return preServiceStart;
	}

	public void setPreServiceStart(final ExecutableDSLEntry preServiceStart) {
		this.preServiceStart = preServiceStart;
	}

	public ExecutableDSLEntry getPreServiceStop() {
		return preServiceStop;
	}

	public void setPreServiceStop(final ExecutableDSLEntry preServiceStop) {
		this.preServiceStop = preServiceStop;
	}

	public ExecutableDSLEntry getStartDetection() {
		return startDetection;
	}

	public void setStartDetection(final ExecutableDSLEntry startDetection) {
		this.startDetection = startDetection;
	}

	public Object getMonitors() {
		return monitors;
	}

	public void setMonitors(final Object monitors) {
		this.monitors = monitors;
	}

	public Object getDetails() {
		return details;
	}

	public void setDetails(final Object details) {
		this.details = details;
	}

	public ExecutableDSLEntry getLocator() {
		return locator;
	}

	public void setLocator(final ExecutableDSLEntry locator) {
		this.locator = locator;
	}

	public ExecutableDSLEntry getStopDetection() {
		return stopDetection;
	}

	public void setStopDetection(final ExecutableDSLEntry stopDetection) {
		this.stopDetection = stopDetection;
	}
}
