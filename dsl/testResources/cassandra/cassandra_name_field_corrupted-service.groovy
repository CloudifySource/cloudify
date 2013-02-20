service {
	nae "cassandra"
	icon "Apache-cassandra-icon.png"

	lifecycle{

		init "cassandra_install.groovy"

		start ([
					"Windows.*": "install\\bin\\cassandra.bat" ])
		postStart "cassandra_create_schema.bat"
  }
  plugins ([
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
            "Memtable Post Flusher Active Count":[
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
          "Memtables Post Flusher Active Tasks",
          "Memtables Post Flusher Pending Tasks",
          "Memtables Post Flusher Completed Tasks"
        ])
      } ,
      metricGroup{
        name "Hinted Handoff"
        metrics ([
          "Hinted Handoff Active Tasks",
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