package com.gigaspaces.cloudify.usm.events;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;

public class AbstractUSMEventListener implements USMEvent {

	protected UniversalServiceManagerBean usm;

	public void init(UniversalServiceManagerBean usm) {
		this.usm = usm;

	}

	public int getOrder() {
		return 5;
	}

}
