package org.cloudifysource.usm.dsl;

import javax.annotation.PostConstruct;

import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.LifecycleEvents;
import org.cloudifysource.usm.events.LifecycleListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.events.StopReason;
import org.springframework.beans.factory.annotation.Autowired;


public class DSLCommandsLifecycleListener extends AbstractUSMEventListener implements LifecycleListener {

	@Autowired(required = true)
	private UniversalServiceManagerConfiguration configuration;
	
	private ServiceLifecycle lifecycle;
	

	@PostConstruct
	public void afterPropertiesSet() {
		this.lifecycle = ((DSLConfiguration) configuration).getService().getLifecycle();
	}
	
	private EventResult executeEntry(Object entry) {
		return new DSLEntryExecutor(entry, this.usm.usmLifecycleBean.getLauncher(), this.usm.getPuExtDir()).run();
	}
	
	public boolean isEventExists(LifecycleEvents event) {
		return this.getEntryForEvent(event) != null;
	}
	
	Object getEntryForEvent(LifecycleEvents event) { 
		switch(event) {
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
