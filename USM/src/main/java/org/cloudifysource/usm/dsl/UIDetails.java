package org.cloudifysource.usm.dsl;

import java.util.HashMap;
import java.util.Map;


import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;
import org.openspaces.ui.UserInterface;



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
		map.put("USM.UI", ui);
		return map;
	}

}
