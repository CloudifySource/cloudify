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
 ******************************************************************************/
package org.cloudifysource.esc.util;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class UniqueFolderNameGeneratorTest {

	private static final int MAX_APPENDER = 2;
	private static final String BASIC_FOLDER_NAME = "testFolderName";

	private File parentFolder = new File(System.getProperty("java.io.tmpdir"));
	
	@Test
	public void testCreateUniqueFolder() {

		try {
			
			String uniqueFolderName = FileUtils.createUniqueFolderName(parentFolder, BASIC_FOLDER_NAME, MAX_APPENDER);
			Assert.assertTrue(uniqueFolderName.equalsIgnoreCase(BASIC_FOLDER_NAME + 1));
			File tempFolder1 = new File(parentFolder, uniqueFolderName);
			tempFolder1.deleteOnExit();
			Assert.assertTrue(tempFolder1.mkdirs());
			
			uniqueFolderName = FileUtils.createUniqueFolderName(parentFolder, BASIC_FOLDER_NAME, MAX_APPENDER);
			Assert.assertTrue(uniqueFolderName.equalsIgnoreCase(BASIC_FOLDER_NAME + 2));
			File tempFolder2 = new File(parentFolder, uniqueFolderName);
			tempFolder2.deleteOnExit();
			Assert.assertTrue(tempFolder2.mkdirs());
			
			uniqueFolderName = FileUtils.createUniqueFolderName(parentFolder, BASIC_FOLDER_NAME, MAX_APPENDER);
			File tempFolderRandom = new File(parentFolder, uniqueFolderName);
			tempFolderRandom.deleteOnExit();
			Assert.assertTrue(tempFolderRandom.mkdirs());
			
		} catch (IOException e) {
			Assert.fail("Creating a unique folder in " + parentFolder.getAbsolutePath() + " failed: " 
					+ e.getMessage());
		}
	}
}
