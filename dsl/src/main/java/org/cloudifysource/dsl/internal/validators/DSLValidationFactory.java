package org.cloudifysource.dsl.internal.validators;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.AppSharedIsolationSLADescriptor;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.SharedIsolationSLADescriptor;
import org.cloudifysource.dsl.internal.DSLValidationException;

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
