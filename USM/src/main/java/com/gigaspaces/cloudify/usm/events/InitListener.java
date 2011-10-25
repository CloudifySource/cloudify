package com.gigaspaces.cloudify.usm.events;

public interface InitListener extends USMEvent {

	EventResult onInit();
	
}
