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


config = new ConfigSlurper().parse(new File("mongos.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.linux

builder = new AntBuilder()
builder.sequential {
	mkdir(dir:config.installDir)
	get(src:osConfig.downloadPath, dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(ServiceUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:config.home)

