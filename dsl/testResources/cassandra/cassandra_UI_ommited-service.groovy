service {
	name "cassandra"
	icon "https://gigaspaces.blob.core.windows.net/packages/cassandra-150x100.png?&se=2015-06-26T00%3A00%3A00Z&sr=b&si=readforever&sig=CNOQcUfUtSV9WyO5JdqZYQWbeliZqI9Zqsawt87pyJg%3D"

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

}