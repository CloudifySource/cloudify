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

	name "activemq"
	type "MESSAGE_BUS"
	icon "feather-small.gif"

	lifecycle{
		init "activemq_install.groovy"
		start "activemq_start.groovy"
//		preStop "activemq_stop.groovy"
	}
	plugins([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [61616],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config([
						"Store Percent Usage": [
							"org.apache.activemq:BrokerName=localhost,Type=Broker",
							"StorePercentUsage"
						],
						port: 11099
					])
		}
	])


	userInterface {
		metricGroups = ([
			metricGroup {

				name "broker"

				metrics([
					"Store Percent Usage",
				])
			},
		]
		)

		widgetGroups = ([
			widgetGroup {
				name "Store Percent Usage"
				widgets ([
					barLineChart{
						metric "Store Percent Usage"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
		]
		)
	}
}


