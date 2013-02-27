package org.cloudifysource.dsl.cloud;

import java.io.File;
import java.util.List;

import org.cloudifysource.dsl.internal.ComputeTemplateHolder;
import org.cloudifysource.dsl.internal.ComputeTemplatesReader;
import org.cloudifysource.dsl.internal.DSLException;
import org.junit.Assert;
import org.junit.Test;

public class ComputeTemplatesReaderTest {
	
	private static final String TEMPLATES_PATH = "src/test/resources/templates/test-template.groovy";
	
	@Test
	public void testComputeTemplatesParsing() throws DSLException {
		
		ComputeTemplatesReader reader = new ComputeTemplatesReader();
		List<ComputeTemplateHolder> cloudTemplatesHolders = reader.readCloudTemplatesFromFile(new File(TEMPLATES_PATH));
		
		Assert.assertEquals(1, cloudTemplatesHolders.size());
		Assert.assertNotNull(cloudTemplatesHolders.get(0).getCloudTemplate());
		
		
		
		
	}

}
