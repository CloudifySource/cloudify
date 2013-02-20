
service {
	name "cassandra-service"
	icon "icon.png"

	lifecycle {

		init "postDeploy.groovy"

		start ([ "Linux":"apache-cassandra-0.7.5/bin/cassandra -f" ,
					"Windows XP": "apache-cassandra-0.7.5\\bin\\cassandra.bat" ,
					"Windows 7": "apache-cassandra-0.7.5\\bin\\cassandra.bat" ])
	}

	plugins ([
		plugin{

			name "Cassandra Metrics"
			className "org.cloudifysource.usm.jmx.JmxMonitor"

			config ([

						"Completed Tasks" : [
							"org.apache.cassandra.db:type=CompactionManager",
							"CompletedTasks"
						],
						"Pending Tasks" : [
							"org.apache.cassandra.db:type=CompactionManager",
							"PendingTasks"
						],
						"Column Family In Progress" : [
							"org.apache.cassandra.db:type=CompactionManager",
							"ColumnFamilyInProgress"
						],
						"Migration Stage Active Count":[
							"org.apache.cassandra.internal:type=StreamStage",
							"ActiveCount"
						],

						"Migration Stage Completed Tasks":[
							"org.apache.cassandra.internal:type=StreamStage",
							"CompletedTasks"
						],

						"Migration Stage Pending Tasks":[
							"org.apache.cassandra.internal:type=StreamStage",
							"PendingTasks"
						],







						port : 8080
					])
		}
	])

	userInterface{

		metricGroups = [
			metricGroup{
				name "process"
				metrics ([
					"Process Cpu Usage",
					"Total Process Virtual Memory",
					"Completed Tasks",
					"Pending Tasks",
					"Column Family In Progress"
				])
			}
		]

		widgetGroups = [
			widgetGroup{
				name "Process Cpu Usage"
				widgets = [
					balanceGauge{ metric "Process Cpu Usage" },
					barLineChart{
						metric "Process Cpu Usage"
						axisYUnit Unit.REGULAR
					}
				]
			},
			widgetGroup{
				name "Total Process Virtual Memory"
				widgets = [
					balanceGauge{ metric "Total Process Virtual Memory" },
					barLineChart{
						metric "Total Process Virtual Memory"
						axisYUnit Unit.REGULAR
					}
				]
			},
			widgetGroup{
				name "Completed Tasks"
				widgets = [
					balanceGauge{ metric "Completed Tasks" },
					barLineChart{
						metric "Completed Tasks"
						axisYUnit Unit.REGULAR
					}
				]
			},
			widgetGroup{
				name "Pending Tasks"
				widgets = [
					balanceGauge{ metric "Pending Tasks" },
					barLineChart{
						metric "Pending Tasks"
						axisYUnit Unit.REGULAR
					}
				]
			},
			widgetGroup{
				name "Column Family In Progress"
				widgets = [
					balanceGauge{ metric "Column Family In Progress" },
					barLineChart{
						metric "Column Family In Progress"
						axisYUnit Unit.REGULAR
					}
				]
			}
		]
	}
}

