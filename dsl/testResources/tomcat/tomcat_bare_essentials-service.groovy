service {
	name "tomcat"

	lifecycle{

		init "tomcat_install.groovy"

		start ([ "Linux":"tomcat_start.groovy" ,
					"Windows.*": "tomcat_start.groovy"
				])

		postStart {
			long start = System.currentTimeMillis();
			while (System.currentTimeMillis() < start + 60000) {
				Socket sock = new Socket();
				println "Checking connection to local tomcat instance on port 80"
				try {
					sock.connect(new InetSocketAddress("127.0.0.1", 80));
					sock.close();
					println "Connection Succeeded!"
					println "*********************"
					return;
				} catch (IOException ioe) {
					println "Connection Failed!"
				} finally {
					try {
						sock.close();
					} catch (IOException e) {
						// ignore
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			println "Could not establish a connection to the local tomcat instance!!"
			throw new IllegalStateException("Could not open a connection to local tomcat on port 80");
		}

		preStop (["Win.*":"catalina-stop.bat",
					"Linux":"./catalina-stop.sh"])
	}
}


