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
import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())
serviceContext = ServiceContextFactory.getServiceContext()
home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"
script = "${home}/bin/catalina"

//start tomcat
println "executing command ${script}"
new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		arg(value:"run")
	}
	exec(executable:"${script}.bat", osfamily:"windows") {
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")        
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		arg(value:"run")
	}
}
