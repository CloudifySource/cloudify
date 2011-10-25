package com.gigaspaces.cloudify.usm.jmx;

import java.util.Map;


import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;

public class JmxDetails extends AbstractJmxPlugin implements Details {

	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config) throws DetailsException {

		return getJmxAttributes();

	}

}
