application {
	name="stockdemo"
	
	service {
		name = "cassandra"	
	}

	service {
		name = "stockAnalyticsMirror"
		dependsOn = ["cassandra"]
	}

	service {
		name = "stockAnalyticsSpace"
		dependsOn = ["stockAnalyticsMirror"]
	}

	service {
		name = "stockAnalyticsProcessor"
		dependsOn = ["stockAnalyticsSpace"]
	}

	service {
		name = "StockDemo"
		dependsOn = ["stockAnalyticsProcessor","stockAnalyticsSpace"]
	}	
			
	service {
		name = "stockAnalytics"
		dependsOn = ["stockAnalyticsSpace"]
	}
	
	service {
		name = "stockAnalyticsFeeder"
		dependsOn = ["stockAnalyticsProcessor"]
	}	
}
