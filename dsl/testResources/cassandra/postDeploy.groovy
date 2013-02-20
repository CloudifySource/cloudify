import  org.openspaces.admin.AdminFactory;
import  org.openspaces.admin.Admin;
import java.util.concurrent.TimeUnit;

def af = new AdminFactory();
def admin = af.createAdmin();
admin.getGridServiceAgents().waitForAtLeastOne(5, TimeUnit.SECONDS)
def agents = admin.getGridServiceAgents().getHostAddress().keySet()
admin.close()
def agentlist = "- 127.0.0.1\n";
agents.each { agentlist += "    - " + it + "\n" }
println "agentlist: " + agentlist
def ip = InetAddress.localHost.hostAddress;
println "ip is:" + ip;
def conf = "apache-cassandra-0.8.0-rc1/conf";
def yaml = new File(conf + "/cassandra.yaml");
println "cassandra yaml location: " + yaml.getAbsolutePath();
def yamltext = yaml.text;
def backup = new File(conf + "/cassandra.yaml_bak");
backup.write yamltext;
yamltext = yamltext.replaceAll("- 127.0.0.1\n", agentlist);
yamltext = yamltext.replaceAll("listen_address: localhost", "listen_address: " + ip);
yamltext = yamltext.replaceAll("rpc_address: localhost", "rpc_address: 0.0.0.0");
yamltext = yamltext.replaceAll("/var/lib/cassandra/data", "../lib/cassandra/data");
yamltext = yamltext.replaceAll("/var/lib/cassandra/commitlog", "../lib/cassandra/commitlog");
yamltext = yamltext.replaceAll("/var/lib/cassandra/saved_caches", "../lib/cassandra/saved_caches");
yaml.write yamltext;
println "wrote new yaml"
def logprops = new File(conf + "/log4j-server.properties");
logpropstext = logprops.text;
logpropstext = logpropstext.replaceAll("/var/log/cassandra/system.log", "../log/cassandra/system.log");
logprops.write logpropstext;
new File("apache-cassandra-0.8.0-rc1/bin/cassandra").setExecutable(true);