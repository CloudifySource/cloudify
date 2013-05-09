package org.cloudifysource.rest;

import org.cloudifysource.rest.controllers.AttributesController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

//Swap the default JUnit4 with the spring specific SpringJUnit4ClassRunner.
//This will allow spring to inject the application context
@RunWith(SpringJUnit4ClassRunner.class)
//Setup the configuration of the application context and the web mvc layer
@ContextConfiguration({"classpath:META-INF/spring/applicationContext.xml",
		"classpath:META-INF/spring/webmvc-config-test.xml" })
public class SpringAutowiredTest {
	@Autowired
	protected ApplicationContext applicationContext;
	
	@Test
	public void test() {
		AttributesController controller = applicationContext.getBean(AttributesController.class);
		System.out.println("controller test finished");
	}
}
