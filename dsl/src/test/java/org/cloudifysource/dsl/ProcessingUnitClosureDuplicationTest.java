/*******************************************************************************
* Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
******************************************************************************/
package org.cloudifysource.dsl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Assert;
import org.junit.Test;

public class ProcessingUnitClosureDuplicationTest {

	private static final String DUPLICATED_SERVICE_FILE_PATH = "testResources/testparsing/duplicatePUService.groovy";
	private final String[] processingUnitTypes = {"lifecycle", "statefulProcessingUnit"
										, "statelessProcessingUnit", "dataGrid"
										, "mirrorProcessingUnit"};
	
	@Test
	public void test() throws IOException {
		File serviceFile = new File(DUPLICATED_SERVICE_FILE_PATH);
		if (!serviceFile.exists()){
			throw new FileNotFoundException("File was not found: " + serviceFile.getAbsolutePath());
		}
        for (String processingUnitType : this.processingUnitTypes) {
            //replace all
            replaceTextInFile(serviceFile, processingUnitTypes[0], processingUnitType);
            verifyServiceFileIsNotParsable(serviceFile);
            for (int j = 1; j < this.processingUnitTypes.length; j++) {
                replaceFirstOccurrenceInFile(serviceFile, processingUnitTypes[j - 1], processingUnitTypes[j]);
                verifyServiceFileIsNotParsable(serviceFile);
            }
        }
		
		//restore file to initial state
		replaceTextInFile(serviceFile, processingUnitTypes[4], processingUnitTypes[0]);
	}

	private void verifyServiceFileIsNotParsable(final File serviceFile) {
		try {
			ServiceReader.readService(serviceFile);
			Assert.assertFalse("Service file was parsed successfully when was suppose to fail. File content was: "
								+ FileUtils.readFileToString(serviceFile), true);
		} catch (PackagingException e) {
			Throwable cause = e.getCause().getCause();
			Assert.assertTrue("Exception was not caused by DSLException", cause instanceof DSLException);
			Assert.assertTrue(
					"the proper exception was not thrown. expecting parsing to fail" 
							+ " due to multiple processingUnit closures in serviceFile"
					, cause.getMessage()
					.contains("There may only be one type of processing unit defined. Found more than one"));
		} catch (DSLException e) {
			Assert.assertFalse("Failed on packaging. Exception was " + ExceptionUtils.getStackTrace(e), true);
		} catch (IOException e) {
			Assert.assertFalse("Failed to read file. Exception was " + ExceptionUtils.getStackTrace(e), true);
		}
	}

	private void replaceTextInFile(final File file, final String target, final String replacement) 
			throws IOException {
		String originalFileContents = FileUtils.readFileToString(file);
		String modified = originalFileContents;
		modified = modified.replace(target,  replacement);
		FileUtils.write(file, modified);
	}
	
	private void replaceFirstOccurrenceInFile(final File file, final String target, final String replacement) 
			throws IOException {
		String originalFileContents = FileUtils.readFileToString(file);
		String modified = originalFileContents;
		modified = modified.replaceFirst(target,  replacement);
		FileUtils.write(file, modified);
	}
}

