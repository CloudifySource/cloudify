/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.openspaces.usm;

import groovy.lang.GroovyShell;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.cloudifysource.usm.launcher.GroovyExceptionHandler;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * This test checks the GroovyExceptionHandler class in-charge of extracting 
 * a groovy exception string from a given input string.
 * The test covers groovy Runtime exceptions and groovy Compilation exceptions.
 * 
 * @author adaml
 *
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class GroovyExceptionHandlerTest {

	//used to wrap the exception string for both ends.
	private String wrapperString = " Some text" 
		+ System.getProperty("line.separator") 
		+ "Some more text ";

	@Test
	public void groovyRuntimeExceptionHandlingTest() throws IOException {
		//Runtime exception string.
		String runtimeExceptionString = "Caught: java.lang.Exception: Some runtime exception" 
										+ System.getProperty("line.separator") 
										+ "at cassandra_install.run(cassandra_install.groovy:55)";
		String wrappedExceptionString = this.wrapperString + runtimeExceptionString + this.wrapperString;

		String exceptionString = GroovyExceptionHandler.getExceptionString(wrappedExceptionString);

		Assert.assertTrue("Runtime exception " + runtimeExceptionString 
				+ " was not properly extracted from the wrapped exception string " 
				, exceptionString.contains(runtimeExceptionString));
		
		
		runtimeExceptionString = "Caught: java.lang.StringIndexOutOfBoundsException: String index out of range: -1"
								+ System.getProperty("line.separator") 
								+ "at file.run(file.groovy:2)";
		
		wrappedExceptionString = this.wrapperString + runtimeExceptionString + this.wrapperString;
		
		exceptionString = GroovyExceptionHandler.getExceptionString(wrappedExceptionString);
		
		Assert.assertTrue("Runtime exception " + runtimeExceptionString 
				+ " was not properly extracted from the wrapped exception string " 
				, exceptionString.contains(runtimeExceptionString));
//		//Use groovyShell to execute a script and assert output contains the exception string. 
//		String groovyInputWithRuntimeError = "import java.lang.Exception"+ System.getProperty("line.separator") + "String s = \"sdf\".substring(4)";
//		String groovyException = generateGroovyException(groovyInputWithRuntimeError);
//		String strippedGroovyException = GroovyExceptionHandler.getExceptionString(groovyException);
//		String expectedResult = "file.groovy: 1: unexpected token: throw @ line 1, column 28";
//		Assert.assertTrue("The exception string " + strippedGroovyException + " does not match the expected exception string "
//				+ expectedResult, strippedGroovyException.contains(expectedResult));
	}


	@Test
	public void groovyCompilationExceptionHandlingTest() throws IOException {
		//Windows groovy compilation exception string
		String winCompilationExceptionString = "SomeDrive:\\GigaSpaces\\gigaspaces-cloudify-2.0.0-m5\\" +
		"work\\processing-units\\petclinic-mongo_mongod_2\\ext\\mongod_install.groovy: 33:" +
		" unexpected token: } @ line 33, column 11";

		String wrappedExceptionString = wrapperString + winCompilationExceptionString + wrapperString;

		String exceptionString = GroovyExceptionHandler.getExceptionString(wrappedExceptionString);

		Assert.assertTrue("Runtime exception " + winCompilationExceptionString 
				+ " was not properly extracted from the wrapped exception string " 
				+ exceptionString
				, exceptionString.equals(winCompilationExceptionString));

		
		
		//Unix groovy compilation exception string
		String unixCompilationExceptionString = "/export/users/adaml/gigaspaces-cloudify-2.0.0-ga/" +
		"tools/groovy/bin/a.groovy: 3: unexpected char: 0xFFFF @ line 3, column 26";

		wrappedExceptionString = wrapperString + unixCompilationExceptionString + wrapperString;

		exceptionString = GroovyExceptionHandler.getExceptionString(wrappedExceptionString);

		Assert.assertTrue("Runtime exception " + winCompilationExceptionString 
				+ " was not properly extracted from the wrapped exception string " 
				+ exceptionString
				, exceptionString.equals(unixCompilationExceptionString));


		
		//Use groovyShell to execute a script and assert output contains the exception string. 
		String groovyInputWithCompilationError = "import java.lang.Exception throw new Exception(Go\");";
		String groovyException = generateGroovyException(groovyInputWithCompilationError);
		String strippedGroovyException = GroovyExceptionHandler.getExceptionString(groovyException);
		String expectedResult = "file.groovy: 1: unexpected token: throw @ line 1, column 28";
		Assert.assertTrue("The exception string " + strippedGroovyException + " does not match the expected exception string "
				+ expectedResult, strippedGroovyException.contains(expectedResult));

	}
	
	private String generateGroovyException(String groovyInput) throws IOException {
		try{
			//create groovy file.
			File groovyFile = new File(System.getProperty("java.io.tmpdir"), "file.groovy");
			// Create file 
			FileWriter fstream = new FileWriter(groovyFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(groovyInput);
			//Close the output stream
			out.close();

			new GroovyShell().run(groovyFile, new ArrayList<String>());
			return "No groovy exception thrown";
		}catch (MultipleCompilationErrorsException e) {
			return e.toString();
		}catch (RuntimeException e){
			return e.toString();
		}
	}


}
