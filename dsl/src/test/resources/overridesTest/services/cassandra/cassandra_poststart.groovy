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
import groovy.util.ConfigSlurper
import org.cloudifysource.dsl.context.ServiceContextFactory

config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())
serviceContext = ServiceContextFactory.getServiceContext()
home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"
script = "${home}/bin/cassandra-cli"

new AntBuilder().sequential {
	exec(executable:script, osfamily:"unix") {
		arg value:"-host"
		arg value:"localhost"
		arg value:"-port"
		arg value:"9160"
		arg value:"-f"
		arg value:"cassandraSchema.txt"
	}
	exec(executable:"${script}.bat", osfamily:"windows") {
		arg value:"-host"
		arg value:"localhost"
		arg value:"-port"
		arg value:"9160"
		arg value:"-f"
		arg value:"cassandraSchema.txt"
	}
	echo(message:"created cassandra schema")
}
