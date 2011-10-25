package com.gigaspaces.cloudify.usm.events;

import com.gigaspaces.cloudify.usm.USMComponent;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;

public interface USMEvent extends USMComponent{

	public void init(UniversalServiceManagerBean usm);
	public int getOrder();
}
