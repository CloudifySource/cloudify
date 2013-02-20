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
package org.cloudifysource.usm;

/*********
 * Contains the results of the execution of an external process, including its exit code and its output stream in string
 * format (which also includes its error stream output).
 * 
 * @author barakme
 * 
 */
public class ExternalProcessResult {

	private int exitValue;
	private String output;

	public ExternalProcessResult(final int exitValue, final String output) {
		super();
		this.exitValue = exitValue;
		this.output = output;
	}

	public int getExitValue() {
		return exitValue;
	}

	public void setExitValue(final int exitValue) {
		this.exitValue = exitValue;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(final String output) {
		this.output = output;
	}

}
