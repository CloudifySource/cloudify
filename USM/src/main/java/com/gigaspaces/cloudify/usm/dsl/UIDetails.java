package com.gigaspaces.cloudify.usm.dsl;

import java.util.HashMap;
import java.util.Map;


import org.openspaces.ui.UserInterface;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;


public class UIDetails implements Details {

	private final UserInterface ui;

	public UIDetails(final UserInterface ui) {
		super();
		this.ui = ui;
	}

	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config)
			throws DetailsException {
		final Map<String, Object> map = new HashMap<String, Object>();
		//map.put("USM.UI", ui);
		return map;
	}

}
