import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.Admin;
import java.util.concurrent.TimeUnit;

//def af = new AdminFactory();
//def admin = af.createAdmin();
//admin.getGridServiceAgents().waitForAtLeastOne(5, TimeUnit.SECONDS)
//def agents = admin.getGridServiceAgents().getHostAddress().keySet()
//admin.close()


//agents.each { agentlist += "    - " + it + "\n" }

def ip = InetAddress.localHost.hostAddress;
println "ip is:" + ip;
def agentlist = "- " + ip + "\n";
println "agentlist: " + agentlist

def conf = "apache-cassandra-0.7.5/conf";
def yaml = new File(conf + "/cassandra.yaml.original");

println "cassandra yaml location: " + yaml.getAbsolutePath();

def yamltext = yaml.text;
// def backup = new File(conf + "/cassandra.yaml_bak");
// backup.write yamltext;
yamltext = yamltext.replaceAll("- 127.0.0.1\n", agentlist);
yamltext = yamltext.replaceAll("listen_address: localhost", "listen_address: " + ip);
yamltext = yamltext.replaceAll("rpc_address: localhost", "rpc_address: 0.0.0.0");
yamltext = yamltext.replaceAll("/var/lib/cassandra/data", "cassandra_storage/cassandra/data");
yamltext = yamltext.replaceAll("/var/lib/cassandra/commitlog", "cassandra_storage/cassandra/commitlog");
yamltext = yamltext.replaceAll("/var/lib/cassandra/saved_caches", "cassandra_storage/cassandra/saved_caches");
def configuration = new File(conf + "/cassandra.yaml");
configuration.write yamltext;
//yaml.write yamltext;
println "wrote new yaml to: " + configuration
def logprops = new File(conf + "/log4j-server.properties");
logpropstext = logprops.text;
logpropstext = logpropstext.replaceAll("/var/log/cassandra/system.log", "cassandra_storage/cassandra/system.log");
logprops.write logpropstext;
//new File("apache-cassandra-0.7.5/bin/cassandra").setExecutable(true);