package org.cloudifysource.usm.jmx;

import java.util.Map;

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;



public class JmxDetails extends AbstractJmxPlugin implements Details {

	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final UniversalServiceManagerConfiguration config) throws DetailsException {

		return getJmxAttributes();

	}

}
