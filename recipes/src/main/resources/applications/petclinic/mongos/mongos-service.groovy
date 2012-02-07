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
service {
	
	name "mongos"
	icon "mongodb.png"
	type "NOSQL_DB"
	numInstances 1
	
	compute {
		template "SMALL_LINUX_32"
	}

	lifecycle {
		install "mongos_install.groovy"
		start "mongos_start.groovy"		
		postStart "mongos_poststart.groovy"
		startDetectionTimeoutSecs 60
		startDetection {
			ServiceUtils.isPortOccupied(context.attributes.thisInstance["port"])
		}
	}
	
	plugins([		        
		plugin {
			name "MongoDBMonitorsPlugin"
			className "org.cloudifysource.mongodb.MongoDBMonitorsPlugin"
			config([				
				"host":"127.0.0.1",				
				"dbName":"admin", 
				"dataSpec":([				    
					"Current Active Connections":"connections.current"					
				])
			])
		}
		
	])

	userInterface {
		metricGroups = ([
			metricGroup {
				name "MongoDB"
				metrics([					
					"Current Active Connections"					
				])
			}
		])

		widgetGroups = ([			
			widgetGroup {
				name "Current Active Connections"
				widgets ([
					balanceGauge{metric = "Current Active Connections"},
					barLineChart{
						metric "Current Active Connections"
						axisYUnit Unit.REGULAR
					},
				])
			}			
		])
	}  
}
