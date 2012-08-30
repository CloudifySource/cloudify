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
import org.hyperic.sigar.OperatingSystem
import org.cloudifysource.dsl.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()

def os = OperatingSystem.getInstance()
def currVendor=os.getVendor()
def stopScript
switch (currVendor) {
	case ["Ubuntu", "Debian", "Mint"]:			
		stopScript="${context.serviceDirectory}/stopOnUbuntu.sh"
		break		
	case ["Red Hat", "CentOS", "Fedora", "Amazon",""]:			
		stopScript="${context.serviceDirectory}/stop.sh"
		break					
	default: throw new Exception("Support for ${currVendor} is not implemented")
}

builder = new AntBuilder()
builder.sequential {
	exec(executable:"${stopScript}", osfamily:"unix")        	
}
