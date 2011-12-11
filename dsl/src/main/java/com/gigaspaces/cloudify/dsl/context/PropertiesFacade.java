package com.gigaspaces.cloudify.dsl.context;

import org.openspaces.core.GigaSpace;

import com.gigaspaces.cloudify.dsl.context.spaceentries.AbstractCloudifyProperty;
import com.gigaspaces.cloudify.dsl.context.spaceentries.CloudifyApplicationProperty;
import com.gigaspaces.cloudify.dsl.context.spaceentries.CloudifyInstanceProperty;
import com.gigaspaces.cloudify.dsl.context.spaceentries.CloudifyServiceProperty;

/**
 * Facade for putting and getting properties over cloudify management space
 * @author eitany
 * @since 2.0
 */
public class PropertiesFacade {

	private final ServiceContext serviceContext;

	private final ContextPropertyAccessor applicationPropertyAccessor = new ContextPropertyAccessor(PropertyContext.APPLICATION);
	private final ContextPropertyAccessor servicePropertyAccessor = new ContextPropertyAccessor(PropertyContext.SERVICE);
	private final ContextPropertyAccessor instancePropertyAccessor = new ContextPropertyAccessor(PropertyContext.INSTANCE);
	
	public PropertiesFacade(ServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	public ContextPropertyAccessor getApplication(){
		return applicationPropertyAccessor;
	}
	
	public ContextPropertyAccessor getService(){
		return servicePropertyAccessor;
	}

	public ContextPropertyAccessor getInstance() {
		return instancePropertyAccessor;
	}

	public class ContextPropertyAccessor {
		private final PropertyContext propertyContext;

		public ContextPropertyAccessor(PropertyContext propertyContext) {
			this.propertyContext = propertyContext;
		}

		//Groovy map operator overload
		public Object putAt(Object key, Object value){
			if (!(key instanceof String))
				throw new IllegalArgumentException("key must be a string");
			
			return put((String) key, value);
		}


		private Object put(String key, Object value) {
			GigaSpace managementSpace = serviceContext.getManagementSpace();
			AbstractCloudifyProperty propertyEntry = preparePropertyTemplate(key);
			AbstractCloudifyProperty previousValue = managementSpace.take(propertyEntry);
			propertyEntry.setValue(value);
			managementSpace.write(propertyEntry);
			return previousValue;
		}
		
		//Groovy map operator overload
		public Object getAt(Object key){
			if (!(key instanceof String))
				throw new IllegalArgumentException("key must be a string");
			
			return get((String) key);
		}
		
		public Object get(String key){
			GigaSpace managementSpace = serviceContext.getManagementSpace();
			AbstractCloudifyProperty propertyEntry = preparePropertyTemplate(key);
			AbstractCloudifyProperty valueEntry = managementSpace.read(propertyEntry);
			return valueEntry != null? valueEntry.getValue() : null;
		}
		
		public boolean containsKey(String key){
			GigaSpace managementSpace = serviceContext.getManagementSpace();
			AbstractCloudifyProperty propertyEntry = preparePropertyTemplate(key);
			return managementSpace.count(propertyEntry) > 0;
		}
		
		
		private AbstractCloudifyProperty preparePropertyTemplate(String key) {
			AbstractCloudifyProperty propertyEntry;
			switch(propertyContext){
				case INSTANCE:
					propertyEntry = prepareInstancePropertyTemplate();
					break;
				case SERVICE:
					propertyEntry = prepareServicePropertyTemplate();
					break;
				case APPLICATION:
					propertyEntry = prepareApplicationPropertyTemplate();
					break;
				default: 
					throw new IllegalArgumentException("Unknown property context " + propertyContext);			
			}
			propertyEntry.setApplicationName(serviceContext.getApplicationName());
			propertyEntry.setKey(key);
			return propertyEntry;
		}
		
		private CloudifyApplicationProperty prepareApplicationPropertyTemplate() {
			return new CloudifyApplicationProperty();
		}

		private CloudifyServiceProperty prepareServicePropertyTemplate() {
			CloudifyServiceProperty serviceProperty = new CloudifyServiceProperty();
			serviceProperty.setServiceName(serviceContext.getServiceName());
			return serviceProperty;
		}

		private CloudifyInstanceProperty prepareInstancePropertyTemplate() {
			CloudifyInstanceProperty instanceProperty = new CloudifyInstanceProperty();
			Integer instanceId = serviceContext.getClusterInfo() != null? serviceContext.getClusterInfo().getInstanceId() : null; 
			instanceProperty.setInstanceId(instanceId);
			instanceProperty.setServiceName(serviceContext.getServiceName());
			return instanceProperty;
		}
		
		
	}
	
}
