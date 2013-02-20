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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CLILocalhostBootstrapperListener implements LocalhostBootstrapperListener {
	
	private final CLIEventsDisplayer displayer;
	private final static String REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|\\s\\[]*(]*+)";
	private final Pattern pattern = Pattern.compile(REGEX);
	private final List<String> localcloudInfoEvents = new ArrayList<String>();

	public CLILocalhostBootstrapperListener(){
		this.displayer = new CLIEventsDisplayer();
	}
	
	@Override
	public void onLocalhostBootstrapEvent(String event) {
		if (event == null){
			displayer.printNoChange();
		}
		else{
			//Check if the event contains a url string. 
			//if it does we classify it as an info event and print it in the end.
			Matcher matcher = pattern.matcher(event);
			if (matcher.find()){
				if (event.contains("Webui")){
					localcloudInfoEvents.add("\t\tCLOUDIFY MANAGEMENT\t" + matcher.group());
				}
				else if (event.contains("Rest")){
					localcloudInfoEvents.add("\t\tCLOUDIFY GATEWAY\t" + matcher.group());
					printBootstrapInfo();
				}
			}else{
				displayer.printEvent(event);
			}
		}
		
	}

	private void printBootstrapInfo() {
		displayer.printEvent("CLOUDIFY LOCAL-CLOUD STARTED");
		displayer.printEvent("");
		displayer.printEvent("LOCAL-CLOUD INFO :");
		for (String eventString : localcloudInfoEvents) {
			displayer.printEvent(eventString);
		}
		
	}

}
