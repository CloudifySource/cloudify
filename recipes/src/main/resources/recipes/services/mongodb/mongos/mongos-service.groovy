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
service {
	
	name "mongos"
	icon "mongodb.png"
	type "NOSQL_DB"
	numInstances 1

	lifecycle {
		init "mongos_install.groovy"
		start "mongos_start.groovy"		
		postStart "mongos_poststart.groovy"
	}
	
	plugins([		
        plugin {
            name "portLiveness"
            className "org.cloudifysource.mongodb.MongoLivenessDetector"
            config ([                
				"port" : 30000,
                "timeoutInSeconds" : 240,
                "host" : "127.0.0.1"
            ])
        }, 
		plugin {
			name "MongoDBMonitorsPlugin"
			className "org.cloudifysource.mongodb.MongoDBMonitorsPlugin"
			config([				
				"host":"127.0.0.1",
				"port" : 30000,
				"dbName":"mydb", 
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
