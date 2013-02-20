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
package org.cloudifysource.shell.installer;

import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

public class CLIEventsDisplayer {

	private final static int PROGRESS_BAR_MAX_LENGTH = 6;
	
	private int progressCounter = 0;
	
	public void printNoChange(){
		System.out.print('.');
		System.out.flush();
		this.progressCounter++;
		if (progressCounter >= PROGRESS_BAR_MAX_LENGTH){
			System.out.print(Ansi.ansi()
					.cursorLeft(PROGRESS_BAR_MAX_LENGTH - 1)
					.eraseLine());
			System.out.flush();
			this.progressCounter = 1;
		}
	}
	
//	public void printNewLine(){
//		System.out.println();
//		System.out.flush();
//		this.progressCounter = 0;
//	}
	
	public void printEvents(final List<String> events){
		for (final String eventString : events) {
			printEvent(eventString);
		}
	}

	public void printEvent(final String eventString) {
		if (progressCounter != 0){
			System.out.println();
			System.out.flush();
			this.progressCounter = 0;
		}
		if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_SUCCESSFULLY)) {
			System.out.println(eventString + " "
					+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_SUCCEED_MESSAGE, Color.GREEN));
		} else if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_FAILED)) {
			System.out.println(eventString + " "
					+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_FAILED_MESSAGE, Color.RED));
		} else {
			System.out.println(eventString);
			System.out.flush();
		}
	}

	public void eraseCurrentLine() {
		System.out.print(Ansi.ansi().cursorLeft(this.progressCounter).eraseLine());
	}
}
