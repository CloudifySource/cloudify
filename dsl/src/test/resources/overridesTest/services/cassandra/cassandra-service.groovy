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
  name serviceName
  icon "Apache-cassandra-icon.png"
  elastic true
  numInstances numInstances
  minAllowedInstances 1
  maxAllowedInstances 3
  type "NOSQL_DB"
  lifecycle{
		init 		lifecycle_init
		preStart 	"cassandra_prestart.groovy"
		start 		"cassandra_start.groovy"
		postStart 	"cassandra_poststart.groovy"
	}
	plugins ([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [7199,9160],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
		plugin {

			name "jmx"

			className "org.cloudifysource.usm.jmx.JmxMonitor"

			config ([
						"Compaction Manager Completed Tasks" :[
							"org.apache.cassandra.db:type=CompactionManager",
							"CompletedTasks"
						],
						"Compaction Manager Pending Tasks" : [
							"org.apache.cassandra.db:type=CompactionManager",
							"PendingTasks"
						],
						"Commit Log Active Tasks": [
							"org.apache.cassandra.db:type=Commitlog",
							"ActiveCount"
						],
						"Commit Log Pending Tasks": [
							"org.apache.cassandra.db:type=Commitlog",
							"PendingTasks"
						],
						"Commit Log Completed Tasks": [
							"org.apache.cassandra.db:type=Commitlog",
							"CompletedTasks"
						],

						"Hinted Handoff Active Count":[
							"org.apache.cassandra.internal:type=HintedHandoff",
							"ActiveCount"
						],
						"Hinted Handoff Completed Tasks":[
							"org.apache.cassandra.internal:type=HintedHandoff",
							"CompletedTasks"
						],
						"Hinted Handoff Pending Tasks":[
							"org.apache.cassandra.internal:type=HintedHandoff",
							"PendingTasks"
						],
						"Memtable Post Flusher Active Tasks":[
							"org.apache.cassandra.internal:type=MemtablePostFlusher",
							"ActiveCount"
						],
						"Memtable Post Flusher Completed Tasks":[
							"org.apache.cassandra.internal:type=MemtablePostFlusher",
							"CompletedTasks"
						],
						"Memtable Post Flusher Pending Tasks":[
							"org.apache.cassandra.internal:type=MemtablePostFlusher",
							"PendingTasks"
						],
						port : 7199
					])
		}
	])

	userInterface{

		metricGroups = ([
			metricGroup{
				name "process"
				metrics ([
					"Process Cpu Usage",
					"Total Process Virtual Memory"
				])
			}  ,
			metricGroup{
				name "Compacation Manager"
				metrics ([
					"Compaction Manager Completed Tasks",
					"Compaction Manager Pending Tasks"
				])
			},
			metricGroup{
				name "Commit Log"
				metrics ([
					"Commit Log Active Tasks",
					"Commit Log Pending Tasks",
					"Commit Log Completed Tasks"
				])
			} ,
			metricGroup{
				name "Memtables Post Flusher"
				metrics ([
					"Memtable Post Flusher Active Tasks",
					"Memtable Post Flusher Pending Tasks",
					"Memtable Post Flusher Completed Tasks"
				])
			} ,
			metricGroup{
				name "Hinted Handoff"
				metrics ([
					"Hinted Handoff Active Count",
					"Hinted Handoff Pending Tasks",
					"Hinted Handoff Completed Tasks"
				])
			}
		]
		)

		widgetGroups = ([
			widgetGroup{
				name "Process Cpu Usage"
				widgets ([
					barLineChart{
						metric "Process Cpu Usage"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
			widgetGroup{
				name "Total Process Virtual Memory"
				widgets ([
					barLineChart{
						metric "Total Process Virtual Memory"
						axisYUnit Unit.MEMORY
					}
				])
			},
			widgetGroup{

				name "Compaction Manager Completed Tasks"
				widgets ([
					barLineChart{

						metric "Compaction Manager Completed Tasks"
						axisYUnit Unit.REGULAR
					}
				])
			},
			widgetGroup{

				name "Compaction Manager Pending Tasks"
				widgets ([
					barLineChart{

						metric "Compaction Manager Pending Tasks"
						axisYUnit Unit.REGULAR
					}
				])
			} ,
			widgetGroup{

				name "Commit Log Active Tasks"
				widgets ([
					barLineChart{

						metric "Commit Log Active Tasks"
						axisYUnit Unit.REGULAR
					}
				])
			}
		]
		)
	}
}