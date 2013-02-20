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
package org.cloudifysource.usm.dsl;

import javax.annotation.PostConstruct;

import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.LifecycleEvents;
import org.cloudifysource.usm.events.LifecycleListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.events.StopReason;
import org.springframework.beans.factory.annotation.Autowired;

/*********
 * A service lifecycle listener that delegates all events to the servcie recipe DSL.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class DSLCommandsLifecycleListener extends AbstractUSMEventListener implements LifecycleListener {

	@Autowired(required = true)
	private ServiceConfiguration configuration;

	private ServiceLifecycle lifecycle;

	/******
	 * BEan initializer.
	 */
	@PostConstruct
	public void afterPropertiesSet() {
		this.lifecycle = configuration.getService().getLifecycle();
	}

	private EventResult executeEntry(final ExecutableDSLEntry entry) {
		return new DSLEntryExecutor(entry, this.usm.getUsmLifecycleBean().getLauncher(), this.usm.getPuExtDir()).run();
	}

	/**********
	 * Indicates if a specific lifecycle event is implemented by this DSL.
	 * 
	 * @param event the event.
	 * @return true if the event is implemented, false otherwise.
	 */
	public boolean isEventExists(final LifecycleEvents event) {
		return this.getEntryForEvent(event) != null;
	}

	Object getEntryForEvent(final LifecycleEvents event) {
		switch (event) {
		case INIT:
			return lifecycle.getInit();
		case PRE_INSTALL:
			return lifecycle.getPreInstall();
		case INSTALL:
			return lifecycle.getInstall();
		case POST_INSTALL:
			return lifecycle.getPostInstall();
		case PRE_START:
			return lifecycle.getPreStart();
		case POST_START:
			return lifecycle.getPostStart();
		case PRE_STOP:
			return lifecycle.getPreStop();
		case STOP:
			return lifecycle.getStop();
		case POST_STOP:
			return lifecycle.getPostStop();
		case SHUTDOWN:
			return lifecycle.getShutdown();
		case PRE_SERVICE_START:
			return lifecycle.getPreServiceStart();
		case PRE_SERVICE_STOP:
			return lifecycle.getPreServiceStop();
		default:
			throw new IllegalArgumentException("Unsupported lifecycle event: " + event);

		}

	}

	@Override
	public EventResult onInit() {
		return executeEntry(lifecycle.getInit());
	}

	@Override
	public EventResult onPreInstall() {
		return executeEntry(lifecycle.getPreInstall());

	}

	@Override
	public EventResult onInstall() {
		return executeEntry(lifecycle.getInstall());
	}

	@Override
	public EventResult onPostInstall() {
		return executeEntry(lifecycle.getPostInstall());

	}

	@Override
	public EventResult onPreStart(final StartReason reason) {
		return executeEntry(lifecycle.getPreStart());

	}

	@Override
	public EventResult onPostStart(final StartReason reason) {
		return executeEntry(lifecycle.getPostStart());
	}

	@Override
	public EventResult onPreStop(final StopReason reason) {
		return executeEntry(lifecycle.getPreStop());

	}
	

	@Override
	public EventResult onPostStop(final StopReason reason) {
		return executeEntry(lifecycle.getPostStop());

	}
	
	@Override
	public EventResult onStop(final StopReason reason) {
		return executeEntry(lifecycle.getStop());

	}


	@Override
	public EventResult onShutdown() {
		return executeEntry(lifecycle.getShutdown());

	}

	@Override
	public EventResult onPreServiceStart() {
		return executeEntry(lifecycle.getPreServiceStart());

	}

	@Override
	public EventResult onPreServiceStop() {
		return executeEntry(lifecycle.getPreServiceStop());

	}

}
