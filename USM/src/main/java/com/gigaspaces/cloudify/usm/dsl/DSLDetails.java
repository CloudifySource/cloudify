package com.gigaspaces.cloudify.usm.dsl;

import java.util.HashMap;
import java.util.Map;


import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;

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
