package org.cloudifysource.usm.dsl;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;



public class DSLDetails implements Details {

	public Map<String, Object> getDetails(UniversalServiceManagerBean usm, UniversalServiceManagerConfiguration config)
			throws DetailsException {
		Service service = ((DSLConfiguration)config).getService();
		HashMap<String, Object> map = new HashMap<String,Object>();

		map.put("icon", service.getIcon());
		//map.put("type", service.getType());
		//map.put("protocolDescription", service.getNetwork() == null ? null : service.getNetwork().getProtocolDescription());
		
		
		return map;
	}

}
