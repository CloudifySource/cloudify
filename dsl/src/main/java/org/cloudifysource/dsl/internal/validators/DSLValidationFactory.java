/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal.validators;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.domain.AppSharedIsolationSLADescriptor;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.GlobalIsolationSLADescriptor;
import org.cloudifysource.domain.IsolationSLA;
import org.cloudifysource.domain.MirrorProcessingUnit;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.ServiceLifecycle;
import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.domain.SharedIsolationSLADescriptor;
import org.cloudifysource.domain.Sla;
import org.cloudifysource.domain.StatelessProcessingUnit;
import org.cloudifysource.domain.TenantSharedIsolationSLADescriptor;
import org.cloudifysource.domain.cloud.AgentComponent;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudProvider;
import org.cloudifysource.domain.cloud.CloudUser;
import org.cloudifysource.domain.cloud.DeployerComponent;
import org.cloudifysource.domain.cloud.DiscoveryComponent;
import org.cloudifysource.domain.cloud.GridComponent;
import org.cloudifysource.domain.cloud.OrchestratorComponent;
import org.cloudifysource.domain.cloud.RestComponent;
import org.cloudifysource.domain.cloud.UsmComponent;
import org.cloudifysource.domain.cloud.WebuiComponent;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.statistics.PercentileInstancesStatisticsConfig;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class DSLValidationFactory {

	private static DSLValidationFactory instance = new DSLValidationFactory();
	private Map<String, Class<? extends DSLValidator>> validatorByClass =
			new HashMap<String, Class<? extends DSLValidator>>();

	private DSLValidationFactory() {
		init();
	}

	public static DSLValidationFactory getInstance() {
		return instance;
	}
	
	private void init() {
		validatorByClass.put(AppSharedIsolationSLADescriptor.class.getName(), 
				AppSharedIsolationSLADescriptorValidator.class);
		validatorByClass.put(Application.class.getName(), ApplicationValidator.class);
		validatorByClass.put(SharedIsolationSLADescriptor.class.getName(), SharedIsolationSLADescriptorValidator.class);
		validatorByClass.put(AgentComponent.class.getName(), AgentComponentValidator.class);
		validatorByClass.put(AppSharedIsolationSLADescriptor.class.getName(), AppSharedIsolationSLADescriptorValidator.class);
		validatorByClass.put(CloudProvider.class.getName(), CloudProviderValidator.class);
		validatorByClass.put(CloudUser.class.getName(), CloudUserValidator.class);
		validatorByClass.put(Cloud.class.getName(), CloudValidator.class);
		validatorByClass.put(ComputeTemplate.class.getName(), ComputeTemplateValidator.class);
		validatorByClass.put(DeployerComponent.class.getName(), DeployerComponentValidator.class);
		validatorByClass.put(DiscoveryComponent.class.getName(), DiscoveryComponentValidator.class);
		validatorByClass.put(GlobalIsolationSLADescriptor.class.getName(), GlobalIsolationSLADescriptorValidator.class);
		validatorByClass.put(GridComponent.class.getName(), GridComponentValidator.class);
		validatorByClass.put(IsolationSLA.class.getName(), IsolationSLAValidatior.class);
		validatorByClass.put(MirrorProcessingUnit.class.getName(), MirrorProcessingUnitValidator.class);
		validatorByClass.put(OrchestratorComponent.class.getName(), OrchestratorComponentValidator.class);
		validatorByClass.put(PercentileInstancesStatisticsConfig.class.getName(), PercentileInstancesStatisticsConfigValidaor.class);
		validatorByClass.put(RestComponent.class.getName(), RestComponentValidator.class);
		validatorByClass.put(ServiceLifecycle.class.getName(), ServiceLifecycleValidator.class);
		validatorByClass.put(ServiceNetwork.class.getName(), ServiceNetworkValidator.class);
		validatorByClass.put(Service.class.getName(), ServiceValidator.class);
		validatorByClass.put(SharedIsolationSLADescriptor.class.getName(), SharedIsolationSLADescriptorValidator.class);
		validatorByClass.put(Sla.class.getName(), SlaValidator.class);
		validatorByClass.put(StatelessProcessingUnit.class.getName(), StatelessProcessingUnitValidator.class);
		validatorByClass.put(TenantSharedIsolationSLADescriptor.class.getName(), TenantSharedIsolationSLADescriptorValidator.class);
		validatorByClass.put(UsmComponent.class.getName(), UsmComponentValidator.class);
		validatorByClass.put(WebuiComponent.class.getName(), WebuiComponentValidator.class);
	}
	
	public static void main(String[] args) throws DSLValidationException {
		DSLValidationFactory factory = new DSLValidationFactory();
		factory.init();
		DSLValidator createValidator = factory.createValidator(new SharedIsolationSLADescriptor());
		
	}
	
	/**
	 * 
	 * @param entity
	 * @return
	 * @throws DSLValidationException
	 */
	public DSLValidator createValidator(final Object entity) throws DSLValidationException {
		Class<? extends DSLValidator> validatorClass = validatorByClass.get(entity.getClass().getName());
		if (validatorClass == null) {
			return null;
		}
		try {
			DSLValidator validator = (DSLValidator) validatorClass.newInstance();
			validator.setDSLEntity(entity);
			return validator;
		} catch (InstantiationException e) {
			throw new DSLValidationException("Failed to load validator for object of type: "
					+ entity.getClass().getName() + ". Error was: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new DSLValidationException("Failed to load validator for object of type: "
					+ entity.getClass().getName() + ". Error was: " + e.getMessage(), e);
		}

	}
}
