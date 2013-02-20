service {
	name "cassandra"
	type "WEB_SERVER"
	lifecycle{

		init "cassandra_install.groovy"

		start ([
					"Windows.*": "install\\bin\\cassandra.bat" ])
		postStart "cassandra_create_schema.bat"
  }}