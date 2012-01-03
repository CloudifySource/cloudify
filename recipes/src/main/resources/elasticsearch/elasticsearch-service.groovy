service {
	
	name "elasticsearch"
	type "NOSQL_DB"
	icon "elasticsearch.jpg"

	lifecycle{
		init "elasticsearch_install.groovy"
		start "elasticsearch_start.groovy"
		preStop "elasticsearch_stop.groovy"
	}
}


