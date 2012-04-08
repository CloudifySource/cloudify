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
service {
	name "tomcatScalingRules"
	numInstances 2
	lifecycle{
		
		init "tomcatScalingRules_install.groovy"
		start "tomcatScalingRules_start.groovy"
		preStop "tomcatScalingRules_stop.groovy"
		
		startDetection {							 
			!ServiceUtils.arePortsFree([8080+context.getInstanceId()-1, 8009+context.getInstanceId()-1] )
		}
	}
	

}
