/*******************************************************************************
* Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
import  org.openspaces.admin.AdminFactory;
import  org.openspaces.admin.Admin;
import org.cloudifysource.dsl.context.ServiceContextFactory
import groovy.util.ConfigSlurper;
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())

//configure the YAML
admin = new AdminFactory().createAdmin()
try {
	admin.getGridServiceAgents().waitForAtLeastOne(5, TimeUnit.SECONDS)
	agents = admin.getGridServiceAgents().getHostAddress().keySet()
	admin.close()
	agentlist = "- 127.0.0.1\n"
	agents.each { agentlist += "    - " + it + "\n" }
	println "agentlist: " + agentlist
	ip = InetAddress.localHost.hostAddress;
	println "ip is:" + ip

  serviceContext = ServiceContextFactory.getServiceContext()
  instanceID = serviceContext.getInstanceId()
  installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID
  home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"
  conf = "${home}/conf"
	yaml = new File("${conf}/cassandra.yaml")
	println "cassandra yaml location: " + yaml.getAbsolutePath()
	yamltext = yaml.text
	backup = new File("${conf}/cassandra.yaml_bak")
	backup.write yamltext
	yamltext = yamltext.replaceAll("- 127.0.0.1\n", agentlist)
	yamltext = yamltext.replaceAll("listen_address: localhost", "listen_address: " + ip)
	yamltext = yamltext.replaceAll("rpc_address: localhost", "rpc_address: 0.0.0.0")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/data", "../lib/cassandra/data")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/commitlog", "../lib/cassandra/commitlog")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/saved_caches", "../lib/cassandra/saved_caches")
	yaml.write yamltext
	println "wrote new yaml"
	logprops = new File(conf + "/log4j-server.properties")
	logpropstext = logprops.text
	logpropstext = logpropstext.replaceAll("/var/log/cassandra/system.log", "../log/cassandra/system.log")
	logprops.write logpropstext
}
finally {
	admin.close();
}