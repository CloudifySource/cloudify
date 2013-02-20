import  org.openspaces.admin.AdminFactory;
import  org.openspaces.admin.Admin;
import java.util.concurrent.TimeUnit;

this.setProperty("cassandraZip", "https://gigaspaces.blob.core.windows.net/packages/apache-cassandra-0.8.0-bin.tar.gz?&se=2015-06-20T00%3A00%3A00Z&sr=b&si=readforever&sig=By6T%2B1XM8pXBrUtKjc%2BrHkAv65GHMUvKHxGYLswpgJQ%3D")
this.setProperty("cassnadraHome", "install")


def zipName = "apache-cassandra-0.8.0"

if (!(new File("apache-cassandra-0.8.0-bin.tar.gz").exists())) {
  
  // download file from storage
  def cassandraZipFile = new FileOutputStream("apache-cassandra-0.8.0-bin.tar.gz")
      def out = new BufferedOutputStream(cassandraZipFile)
      out << new URL(cassandraZip).openStream()
      out.close()
}


new AntBuilder().sequential {  // create an antbuilder

	mkdir(dir:cassnadraHome)

	gunzip(src:"apache-cassandra-0.8.0-bin.tar.gz", 
		dest:cassnadraHome + "/apache-cassandra-0.8.0-bin.tar"  )
			
	untar(src:cassnadraHome + "/" + "apache-cassandra-0.8.0-bin.tar", dest:cassnadraHome)

	move(todir: cassnadraHome, overwrite:true) {
		fileset(dir:cassnadraHome + "/" + zipName, includes:"**/*")
	}

	delete(file:"${zipName}-bin.tar.gz") 
	delete(dir:"${cassnadraHome}/${zipName}") 
}
//configure the YAML
def af = new AdminFactory();
def admin = af.createAdmin();
try {
admin.getGridServiceAgents().waitForAtLeastOne(5, TimeUnit.SECONDS)
def agents = admin.getGridServiceAgents().getHostAddress().keySet()
admin.close()
def agentlist = "- 127.0.0.1\n";
agents.each { agentlist += "    - " + it + "\n" }
println "agentlist: " + agentlist
def ip = InetAddress.localHost.hostAddress;
println "ip is:" + ip;

def conf = "${cassnadraHome}/conf";
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
new File("${cassnadraHome}/bin/cassandra").setExecutable(true);
}
finally {
  admin.close();
}

System.exit(0);
