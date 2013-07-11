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
package org.cloudifysource.usm.dsl;

import java.util.Collections;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.cloudifysource.domain.LifecycleEvents;
import org.cloudifysource.domain.ServiceLifecycle;
import org.cloudifysource.domain.entry.ExecutableDSLEntry;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.cloudifysource.usm.events.EventResult;
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

	private boolean debugAllEvents;

	private final Set<LifecycleEvents> debugEvents = Collections.emptySet();

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DSLCommandsLifecycleListener.class.getName());

	/******
	 * BEan initializer.
	 */
	@PostConstruct
	public void afterPropertiesSet() {
		this.lifecycle = configuration.getService().getLifecycle();
	}


	private EventResult executeEntry(final ExecutableDSLEntry entry, final LifecycleEvents event) {
		return new DSLEntryExecutor(entry, this.usm.getUsmLifecycleBean().getLauncher(), this.usm.getPuExtDir(), event)
				.run();
	}

	/**********
	 * Indicates if a specific lifecycle event is implemented by this DSL.
	 *
	 * @param event
	 *            the event.
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
		return executeEntry(lifecycle.getInit(), LifecycleEvents.INIT);
	}

	@Override
	public EventResult onPreInstall() {
		return executeEntry(lifecycle.getPreInstall(), LifecycleEvents.PRE_INSTALL);

	}

	@Override
	public EventResult onInstall() {
		return executeEntry(lifecycle.getInstall(), LifecycleEvents.INSTALL);
	}

	@Override
	public EventResult onPostInstall() {
		return executeEntry(lifecycle.getPostInstall(), LifecycleEvents.POST_INSTALL);

	}

	@Override
	public EventResult onPreStart(final StartReason reason) {
		return executeEntry(lifecycle.getPreStart(), LifecycleEvents.PRE_START);

	}

	@Override
	public EventResult onPostStart(final StartReason reason) {
		return executeEntry(lifecycle.getPostStart(), LifecycleEvents.POST_START);
	}

	@Override
	public EventResult onPreStop(final StopReason reason) {
		return executeEntry(lifecycle.getPreStop(), LifecycleEvents.PRE_STOP);

	}

	@Override
	public EventResult onPostStop(final StopReason reason) {
		return executeEntry(lifecycle.getPostStop(), LifecycleEvents.POST_STOP);

	}

	@Override
	public EventResult onStop(final StopReason reason) {
		return executeEntry(lifecycle.getStop(), LifecycleEvents.STOP);

	}

	@Override
	public EventResult onShutdown() {
		return executeEntry(lifecycle.getShutdown(), LifecycleEvents.SHUTDOWN);

	}

	@Override
	public EventResult onPreServiceStart() {
		return executeEntry(lifecycle.getPreServiceStart(), LifecycleEvents.PRE_SERVICE_START);

	}

	@Override
	public EventResult onPreServiceStop() {
		return executeEntry(lifecycle.getPreServiceStop(), LifecycleEvents.PRE_SERVICE_START);

	}

	}
