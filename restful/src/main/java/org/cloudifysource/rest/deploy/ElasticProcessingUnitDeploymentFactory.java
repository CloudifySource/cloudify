/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.deploy;

import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;

/**
 * an interface for creating a new elastic deployment object.
 * @author adaml
 * @since 2.6.0
 */
public interface ElasticProcessingUnitDeploymentFactory {
	
	/**
	 * create a new elastic deployment object using deployment config object
	 * @param deploymentConfig
	 * 			the deployment configuration object.
	 * @return
	 * 			an elastic deployment object
	 * @throws org.cloudifysource.rest.deploy.ElasticDeploymentCreationException .
	 */
	ElasticDeploymentTopology create(final DeploymentConfig deploymentConfig)
			throws ElasticDeploymentCreationException;
}
