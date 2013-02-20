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
package org.cloudifysource.dsl.context.kvstorage;

import groovy.lang.GroovyObjectSupport;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.context.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ServiceCloudifyAttribute;

/*********
 * .
 * @author Eitan
 *
 */
public class ServiceAttributesAccessor extends AbstractAttributesAccessor {

	private final String serviceName;
	private final InstancesFacade instancesFacade;

	public ServiceAttributesAccessor(final AttributesFacade attributesFacade, final String applicationName,
			final String serviceName, final ServiceContext serviceContext) {
		super(attributesFacade, applicationName);
		this.serviceName = serviceName;
		this.instancesFacade = new InstancesFacade(attributesFacade, applicationName, serviceName, serviceContext);
	}

	@Override
	protected ServiceCloudifyAttribute prepareAttributeTemplate() {
		final ServiceCloudifyAttribute attribute = new ServiceCloudifyAttribute();
		attribute.setServiceName(serviceName);
		return attribute;
	}

	public InstancesFacade getInstances() {
		return instancesFacade;
	}

	/************
	 * .
	 * @author Eitan.
	 *
	 */
	// This is serializable just because groovy .each method returns the iterator as a result, if the user
	// will write each method as a last command in a closue with no other return value he will get serialization error
	public static class InstancesFacade extends GroovyObjectSupport implements Iterable<InstanceAttributesAccessor>,
			Serializable {

		private static final long serialVersionUID = 1L;

		private static final int WAIT_FOR_SERVICE_TIMEOUT = 10;
		private final transient ServiceContext serviceContext;
		private final transient AttributesFacade attributesFacade;
		private final transient String applicationName;
		private final transient String serviceName;

		public InstancesFacade(final AttributesFacade attributesFacade, final String applicationName,
				final String serviceName, final ServiceContext serviceContext) {
			this.attributesFacade = attributesFacade;
			this.applicationName = applicationName;
			this.serviceName = serviceName;
			this.serviceContext = serviceContext;
		}

		@Override
		public Iterator<InstanceAttributesAccessor> iterator() {
			return new InstanceFacadeIterator();
		}

		/********
		 * Accessor.
		 * @param key .
		 * @return .
		 */
		public Object getAt(final Object key) {
			if (!(key instanceof Integer)) {
				throw new IllegalArgumentException("key must be integer and represent service instance id");
			}

			final Integer instanceId = (Integer) key;
			return new InstanceAttributesAccessor(attributesFacade, applicationName, serviceName, instanceId);
		}

		@Override
		public String toString() {
			return "";
		}

		/************
		 * .
		 * @author barakme
		 *
		 */
		public class InstanceFacadeIterator implements Iterator<InstanceAttributesAccessor>, Serializable {

			private static final long serialVersionUID = 1L;

			private final transient int instancesCount;
			private transient int currentInstanceIndex = 0;

			public InstanceFacadeIterator() {
				final Service service = serviceContext.waitForService(serviceContext.getServiceName(),
						WAIT_FOR_SERVICE_TIMEOUT,
						TimeUnit.SECONDS);
				instancesCount = service != null ? service.getNumberOfPlannedInstances() : 0;
			}

			@Override
			public boolean hasNext() {
				return currentInstanceIndex < instancesCount;
			}

			@Override
			public InstanceAttributesAccessor next() {
				final int instanceId = currentInstanceIndex + 1;
				currentInstanceIndex++;
				return new InstanceAttributesAccessor(attributesFacade, applicationName, serviceName, instanceId);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String toString() {
				return "";
			}
		}
	}

}
