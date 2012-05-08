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
package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.openspaces.admin.space.ElasticSpaceDeployment;

/**
 * Defines an elastic deployment of a partitioned data grid (space). The datagrid Processing unit configuration POJO is
 * initialized by the service groovy DSL and holds all the required information regarding the deployment of datagrid
 * processing units.
 * 
 * In order to deploy mirror based services, use this processing unit type.
 * 
 * @see ElasticSpaceDeployment
 * 
 * @author adaml
 * 
 */
@CloudifyDSLEntity(name = "dataGrid", clazz = DataGrid.class, allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class DataGrid extends ServiceProcessingUnit {

}
