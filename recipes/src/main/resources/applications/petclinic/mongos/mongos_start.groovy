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

config = new ConfigSlurper().parse(new File('mongos.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
mongoService = serviceContext.waitForService("mongoConfig", 20, TimeUnit.SECONDS)
mongoInstances = mongoService.waitForInstances(mongoService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
cfgHost = mongoInstances[0].hostAddress

new AntBuilder().exec(executable:config.script) {
	arg line:"--configdb ${cfgHost}:${config.cfgPort}"
	arg line:"--port ${config.port}"
	arg line:"--chunkSize 1"
}
