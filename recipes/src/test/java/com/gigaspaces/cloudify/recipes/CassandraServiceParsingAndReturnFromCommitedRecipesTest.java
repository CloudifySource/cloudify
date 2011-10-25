package com.gigaspaces.cloudify.recipes;

import java.io.File;

import org.junit.Test;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class CassandraServiceParsingAndReturnFromCommitedRecipesTest {
	
	private final static String LEGAL_RESOURCES_PATH = "target/classes/cassandra/";
	private String nameInGroovy = "cassandra";
	private File cassandraDslFile;
	private File cassandraWorkDir;
	private Service service;
	
	@Test
	public void fullyConfigedGroovy() throws Exception
    {
		cassandraDslFile = new File(LEGAL_RESOURCES_PATH + "cassandra-service.groovy");
		cassandraWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	@Test
	public void getServiceFromDirInvocation() throws Exception
    {
		cassandraWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromDirectory(cassandraWorkDir).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
}