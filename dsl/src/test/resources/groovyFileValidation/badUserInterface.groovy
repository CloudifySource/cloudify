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
*******************************************************************************/

/*
 * Validation for this service file should fail because The axisYUnit
 * is not of type org.openspaces.ui.Unit
 */
service {
	name "junit"
	icon "junit.gif"
	type "UNDEFINED"
	
    elastic true
	numInstances 1
		
	compute {
		template "SMALL_LINUX"
	}

	lifecycle {	
	
		install "junit.groovy"
		start "junit.groovy"		
		preStop "junit.groovy"
		
		startDetectionTimeoutSecs 240
		startDetection {
		}	
		
		
		postStart {
									
		}
		
		postStop {
		
		}		
	}
	//an example of a bad Unit type.
	userInterface {

		metricGroups = ([
			metricGroup {

				name "process"

				//this is where ew are suppose to fail
				metrics(["Total Process Cpu Time","Process Cpu Usage","Total Process Virtual Memory","Num Of Active Threads",["overrriddenYAxisMetric", "nonUnitObj"]])
			} ,
			metricGroup {

				name "http"

				metrics(["Current Http Threads Busy","Current Http Thread Count","Backlog","Total Requests Count"])
			} ,

		]
		)
	}
}