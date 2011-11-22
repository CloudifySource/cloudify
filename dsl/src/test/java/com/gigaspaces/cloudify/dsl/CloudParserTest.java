package com.gigaspaces.cloudify.dsl;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class CloudParserTest {

	private final static String SIMPLE_CLOUD_PATH = "testResources/simple/my-cloud.groovy";
	
	@Ignore
	@Test
	public void testCloudParser() throws Exception {
		Cloud2 cloud = ServiceReader.readCloud(new File(SIMPLE_CLOUD_PATH));
		assertNotNull(cloud);
		assertNotNull(cloud.getProvider());
		assertNotNull(cloud.getTemplates());
		assertNotNull(cloud.getUser());
		assertNotNull(cloud.getTemplates().size() > 0);
		assertNotNull(cloud.getTemplates().get("SMALL_LINUX_32"));
	}


}
