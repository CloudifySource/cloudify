service {
	name "tomcat"
	icon "tomcat.gif"
	type "WEB_SERVER"
	
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

	//adjust http port 80 to confirm to -Dweb.port=80 in catalina-run.bat
	plugins([
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config([
						"Current Http Threads Busy": [
							"Catalina:type=ThreadPool,name=\"http-bio-80\"",
							"currentThreadsBusy"
						],
						"Current Http Threads count": [
							"Catalina:type=ThreadPool,name=\"http-bio-80\"",
							"currentThreadCount"
						],
						"Backlog": [
							"Catalina:type=ProtocolHandler,port=80",
							"backlog"
						],
						"Active Sessions":[
							"Catalina:type=Manager,context=/travel,host=localhost",
							"activeSessions"
						],
						port: 9999
					])
		}
	])
		
	network {
		port = 80
		protocolDescription ="HTTP"
	}
}


