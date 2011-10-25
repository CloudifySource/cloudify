service {
	
	name "elasticsearch"
	type "NOSQL_DB"
	icon "http://http://profile.ak.fbcdn.net/hprofile-ak-snc4/50334_241854170154_231615_n.jpg"

	lifecycle{
		init "elasticsearch_install.groovy"
		start "elasticsearch_start.groovy"
		preStop "elasticsearch_stop.groovy"
	}
}


