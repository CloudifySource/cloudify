/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.validators;

import org.cloudifysource.domain.Application;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Test;

public class ValidateApplicationNameTest {
	
	@Test
	public void testInvalidName1() {
		test("name[");
	}
	
	@Test
	public void testInvalidName2() {
		test("]");
	}
	
	@Test
	public void testInvalidName3() {
		test("name_)");
	}
	
	@Test
	public void testInvalidName4() {
		test("(name");
	}
	
	@Test
	public void testInvalidName5() {
		test("{}");
	}
	
	private void test(final String name) {
		Application application = new Application();
		application.setName(name);
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		context.setApplication(application);
		ValidatorsTestsUtils.validate(
				new ValidateApplicationName(), 
				context, 
				CloudifyMessageKeys.APPLICATION_NAME_CONTAINS_INVALID_CHARS.getName());
	}

}
