package org.cloudifysource.usm.events;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.UniversalServiceManagerBean;

public interface USMEvent extends USMComponent{

	public void init(UniversalServiceManagerBean usm);
	public int getOrder();
}
