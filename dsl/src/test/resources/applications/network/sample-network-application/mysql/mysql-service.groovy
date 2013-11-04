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
	name "mysql"
	type "DB_SERVER"
	numInstances 2
	
	compute {
		template "SMALL_UBUNTU"
	}

	lifecycle{
		install 'printContext.groovy'
	}

	network {
		accessRules {
			incoming = ([
				accessRule {
					portRange "3306"
					type "APPLICATION"
				},
				accessRule {
					portRange "3307"
					type "SERVICE"
				}
			])
		}

		
	}
}

