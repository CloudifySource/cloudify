package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Defines an elastic deployment of a partitioned data grid (space).
 * The datagrid Processing unit configuration POJO is initialized by 
 * the service groovy DSL and holds all the required information regarding 
 * the deployment of datagrid processing units.
 *  
 * In order to deploy mirror based services, use this processing unit type.
 *  
 * @see ElasticSpaceDeployment
 * 
 * @author adaml
 *
 */
@CloudifyDSLEntity(name="dataGrid", clazz=DataGrid.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class DataGrid extends ServiceProcessingUnit{

	
}
