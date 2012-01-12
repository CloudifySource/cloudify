package org.cloudifysource.recipes;

import java.net.HttpURLConnection;
import java.net.URL;

import org.cloudifysource.dsl.Service;

import junit.framework.Assert;


public class ServiceTestUtil {
	
	// private constructor to prevent instantiation
	private ServiceTestUtil(){}
	
	static public void validateIcon(Service service) throws Exception {
				
		String icon = service.getIcon();
		if(icon.startsWith("http")){
			HttpURLConnection connection = (HttpURLConnection) new URL(icon).openConnection();
			connection.setRequestMethod("HEAD");
			Assert.assertEquals("The icon URL cannot establish a connection" , 
						 HttpURLConnection.HTTP_OK , connection.getResponseCode());
			connection.disconnect();
		}
	}
	
	static public void validateName(Service service , String serviceName) throws Exception {
		Assert.assertNotNull(service);
		Assert.assertTrue("Service name isn't correct" , service.getName().compareTo(serviceName) == 0);
	}
}	
