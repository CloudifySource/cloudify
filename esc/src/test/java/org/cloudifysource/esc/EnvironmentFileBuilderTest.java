/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.esc.installer.EnvironmentFileBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the EnvironmentFileBuilder.
 * @author adaml
 *
 */
public class EnvironmentFileBuilderTest {

	
	private static final String TESTING_VALUE = "TESTING_VALUE";
	private static final String EXTERNAL_ENV_VAR_1 = "EXTERNAL_ENV_VAR_1";
	private static final String EXTERNAL_ENV_VALUE_1 = "EXTERNAL_ENV_VALUE_1";
	private static final String EXTERNAL_ENV_VAR_2 = "EXTERNAL_ENV_VAR_2";
	private static final String EXTERNAL_ENV_VALUE_2 = "EXTERNAL_ENV_VALUE_2";
	private static final String EXTERNAL_ENV_VAR_3 = "EXTERNAL_ENV_VAR_3";
	private static final String EXTERNAL_ENV_VALUE_3 = "EXTERNAL_ENV_VALUE_3";
	
	private static final String INTERNAL_ENV_VAR_1 = "INTERNAL_ENV_VAR_1";
	private static final String INTERNAL_ENV_VALUE_1 = "INTERNAL_ENV_VALUE_1";

	@Test 
	public void testLinuxEnvironmentValidity() {
		final Map<String, String> externalEnvVars = createExternalEnvFile();
		final EnvironmentFileBuilder linuxEnvFileBuilder = 
					new EnvironmentFileBuilder(ScriptLanguages.LINUX_SHELL, externalEnvVars);
		//add a simple var. this variable is not contained in the external env var map
		linuxEnvFileBuilder.exportVar(INTERNAL_ENV_VAR_1, INTERNAL_ENV_VALUE_1);
		//add a value to an env var that is contained in the external environment var map.
		linuxEnvFileBuilder.exportVar(EXTERNAL_ENV_VAR_1, TESTING_VALUE);
		String environment = linuxEnvFileBuilder.build().toString();
		
		assertLinuxValidEnvVarString(environment);
		
	}
	
	@Test 
	public void testWindowsEnvironmentValidity() {
		final Map<String, String> externalEnvVars = createExternalEnvFile();
		final EnvironmentFileBuilder windowsEnvFileBuilder = 
					new EnvironmentFileBuilder(ScriptLanguages.WINDOWS_BATCH, externalEnvVars);
		//add a simple var. this variable is not contained in the external env var map
		windowsEnvFileBuilder.exportVar(INTERNAL_ENV_VAR_1, INTERNAL_ENV_VALUE_1);
		//add a value to an env var that is contained in the external environment var map.
		windowsEnvFileBuilder.exportVar(EXTERNAL_ENV_VAR_1, TESTING_VALUE);
		String environment = windowsEnvFileBuilder.build().toString();
		
		assertWindowsValidEnvVarString(environment);
		
	}
	
	@Test 
	public void testEnvironmentFileName() {
		final EnvironmentFileBuilder winEnvFileBuilder = new EnvironmentFileBuilder(ScriptLanguages.WINDOWS_BATCH, 
															new HashMap<String, String>());
		final EnvironmentFileBuilder linuxEnvFileBuilder = new EnvironmentFileBuilder(ScriptLanguages.LINUX_SHELL, 
				new HashMap<String, String>());
		
		Assert.assertTrue("env file name mismatch", 
				winEnvFileBuilder.getEnvironmentFileName().equals("cloudify_env.bat"));
		Assert.assertTrue("env file name mismatch", 
				linuxEnvFileBuilder.getEnvironmentFileName().equals("cloudify_env.sh"));
	}

	//assert quotes surrounding the value and that external values are being attached, not overridden. 
	private void assertWindowsValidEnvVarString(final String environment) {
		//assert value was set without overrides.
		Assert.assertTrue("Expecting internal value " + INTERNAL_ENV_VAR_1 + " to be exported as-is",
				environment.contains("SET \"" + INTERNAL_ENV_VAR_1 + "=" + INTERNAL_ENV_VALUE_1 + "\""));
		//assert the external value was appended to the env var; 
		Assert.assertTrue("Value " + EXTERNAL_ENV_VALUE_1 + " was not appended to the internal value.",
				environment.contains("SET \"" + EXTERNAL_ENV_VAR_1 + "=" 
									+ TESTING_VALUE + ' ' + EXTERNAL_ENV_VALUE_1 + "\""));
		//assert the external value does not override the above value;
		Assert.assertFalse(EXTERNAL_ENV_VAR_1 + " was overridden.",
				environment.contains("SET \"" + EXTERNAL_ENV_VAR_1 + "=" 
									+ EXTERNAL_ENV_VALUE_1 + "\""));
		
		//assert rest of the external environment vars were set
		Assert.assertTrue("External var " + EXTERNAL_ENV_VAR_2 + " were not set.",
				environment.contains("SET \"" + EXTERNAL_ENV_VAR_2 + "=" + EXTERNAL_ENV_VALUE_2 + "\""));
		Assert.assertTrue("External var " + EXTERNAL_ENV_VAR_3 + " were not set.", 
				environment.contains("SET \"" + EXTERNAL_ENV_VAR_3 + "=" + EXTERNAL_ENV_VALUE_3 + "\""));
	}

	//assert quotes surrounding the value and that external values are being attached, not overridden. 
	private void assertLinuxValidEnvVarString(final String environment) {
		//assert value was set without overrides.
		Assert.assertTrue("Expecting internal value " + INTERNAL_ENV_VAR_1 + " to be exported as-is",
				environment.contains("export \"" + INTERNAL_ENV_VAR_1 + "=" + INTERNAL_ENV_VALUE_1 + "\""));
		//assert the external value was appended to the env var; 
		Assert.assertTrue("Value " + EXTERNAL_ENV_VALUE_1 + " was not appended to the internal value.",
				environment.contains("export \"" + EXTERNAL_ENV_VAR_1 + "=" 
									+ TESTING_VALUE + ' ' + EXTERNAL_ENV_VALUE_1 + "\""));
		//assert the external value does not override the above value;
		Assert.assertFalse(EXTERNAL_ENV_VAR_1 + " was overridden.", 
				environment.contains("export \"" + EXTERNAL_ENV_VAR_1 + "=" + EXTERNAL_ENV_VALUE_1 + "\""));
		
		//assert rest of the external environment vars were exported
		Assert.assertTrue("External var " + EXTERNAL_ENV_VAR_2 + " were not set.",
				environment.contains("export \"" + EXTERNAL_ENV_VAR_2 + "=" + EXTERNAL_ENV_VALUE_2 + "\""));
		Assert.assertTrue("External var " + EXTERNAL_ENV_VAR_3 + " were not set.",
				environment.contains("export \"" + EXTERNAL_ENV_VAR_3 + "=" + EXTERNAL_ENV_VALUE_3 + "\""));
	}

	private Map<String, String> createExternalEnvFile() {
		final Map<String, String> envMap = new HashMap<String, String>();
		envMap.put(EXTERNAL_ENV_VAR_1, EXTERNAL_ENV_VALUE_1);
		envMap.put(EXTERNAL_ENV_VAR_2, EXTERNAL_ENV_VALUE_2);
		envMap.put(EXTERNAL_ENV_VAR_3, EXTERNAL_ENV_VALUE_3);
		return envMap;
	}
}
