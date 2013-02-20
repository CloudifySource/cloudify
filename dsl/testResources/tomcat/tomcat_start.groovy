import org.openspaces.admin.AdminFactory;
import java.util.concurrent.TimeUnit;


this.setProperty("catalinaHome", "install")
this.setProperty("javaHome", System.getProperty("java.home"))

//create the admin instance and get the cassandra port
def admin = new AdminFactory().useDaemonThreads(true).create();

try {
	println("waiting for cassandra")
	def pu = admin.getProcessingUnits().waitFor("cassandra",20,TimeUnit.SECONDS);
	if (pu == null) {
		throw new IllegalStateException("Cannot locate cassandra Service");
	}
	if (!pu.waitFor(1,20,TimeUnit.SECONDS)) {
		throw new IllegalStateException("Cannot locate a cassandra service instance");
	}
	def cassandraIP = pu.getInstances()[0].getGridServiceContainer().getMachine().getHostAddress();
	println("got cassandra ip ${cassandraIP}")

	//start tomcat
	def builder = new ProcessBuilder()
	builder.environment().put("CATALINA_HOME", catalinaHome)
	builder.environment().put("CASSANDRA_IP",cassandraIP)
	if(ServiceUtils.isWindows()) {
		builder.command("catalina-run.bat")
	} else {
		new File("catalina-run.sh").setExecutable(true);
		builder.command("./catalina-run.sh")
	}
	Process p = builder.start()
	p.consumeProcessErrorStream(System.err);
	p.consumeProcessOutputStream(System.out)
	p.waitFor()
}
finally {
	admin.close();
}