package org.cloudifysource.dsl.internal.validators;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.IsolationSLA;
import org.cloudifysource.dsl.MetricGroup;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceTierType;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.openspaces.ui.Unit;

public class ServiceValidator implements DSLValidator {

	private Service entity;

	@Override
	public void setDSLEntity(final Object dslEntity) {
		this.entity = (Service) dslEntity;
		
	}
	
	@DSLValidation
	public void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		validateInstanceNumber();
		validateServiceType();
	}

	@DSLValidation
	public void validateRetriesLimit(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getRetries() < -1) {
			throw new DSLValidationException(
					"Valid values for the retries field are -1 (for always retry) and above. "
							+ "Set to '0' for no retries.");
		}
	}

	@DSLValidation
	public void validateMultitenantStorage(final DSLValidationContext validationContext) throws DSLValidationException {

		if (entity.getNumInstances() > 1 && isMultitenant() && entity.getStorage().getTemplate() != null) {
			throw new DSLValidationException(
					"numInstances must be 1 when used in a Multi-tenant configuration with static storage defined.");
		}
	}

	private boolean isMultitenant() {
		final IsolationSLA isolationSla = entity.getIsolationSLA();
		if (isolationSla != null) {
			if (isolationSla.getDedicated() != null) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	private void validateServiceType() throws DSLValidationException {
		boolean typeExists = false;
		final String[] enumAsString = new String[ServiceTierType.values().length];
		int counter = 0;
		for (final ServiceTierType tierType : ServiceTierType.values()) {
			enumAsString[counter] = tierType.toString();
			counter++;
			if (tierType.toString().equalsIgnoreCase(this.entity.getType())) {
				typeExists = true;
				break;
			}
		}
		if (!typeExists) {
			throw new DSLValidationException("The service type '" + this.entity.getType() + "' is undefined."
					+ "The known service types include " + Arrays.toString(enumAsString));
		}
	}

	private void validateInstanceNumber() throws DSLValidationException {
		if (this.entity.getNumInstances() > this.entity.getMaxAllowedInstances()) {
			throw new DSLValidationException("The requested number of instances ("
					+ this.entity.getNumInstances() + ") exceeds the maximum number of instances allowed"
					+ " (" + this.entity.getMaxAllowedInstances() + ") for service " + this.entity.getName() + ".");
		}
	}

	/**
	 * Validate the icon property (if set) points to an existing file.
	 *
	 * @param validationContext
	 *            The DSLValidationContext object
	 * @throws DSLValidationException
	 *             Indicates the icon could not be found
	 */
	@DSLValidation
	public void validateIcon(final DSLValidationContext validationContext) throws DSLValidationException {
		boolean isServiceFile = false;
		final String serviceSuffix = DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX.trim();
		final String filePath = validationContext.getFilePath().trim();

		// execute this validation only if an icon was set and this is a Service's groovy file (not an Application's)
		if (filePath.endsWith(serviceSuffix)) {
			isServiceFile = true;
		}

		if (this.entity.getIcon() != null && isServiceFile) {
			final File dslFile = new File(filePath);
			File iconFile = new File(dslFile.getParent(), this.entity.getIcon());

			if (!iconFile.isFile()) {

				// check the icon at the extended path location, required for service that extend another Service file

				File dslDirectory = dslFile.getParentFile();
				if (this.entity.getExtendedServicesPaths() != null) {
					for (final String extendedPath : this.entity.getExtendedServicesPaths()) {
						if (isAbsolutePath(extendedPath)) {
							dslDirectory = new File(extendedPath);
						} else {
							// climb up the extend hierarchy to look for the icno
							dslDirectory = new File(dslDirectory.getAbsolutePath() + "/" + extendedPath);
						}
						iconFile = new File(dslDirectory.getAbsolutePath(), this.entity.getIcon());
						if (iconFile.isFile()) {
							break;
						}
					}
				}

				if (!iconFile.isFile()) {
					throw new DSLValidationException("The icon file \"" + iconFile.getAbsolutePath()
							+ "\" does not exist.");
				}

			}
		}

	}

	private boolean isAbsolutePath(final String extendedPath) {
		return new File(extendedPath).isAbsolute();
	}

	@DSLValidation
	public void validateCustomProperties(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getCustomProperties().containsKey(CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT)) {
			try {
				Long.parseLong(this.entity.getCustomProperties()
						.get(CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT));

			} catch (final NumberFormatException e) {
				throw new DSLValidationException("The "
						+ CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT
						+ " property must be a long value", e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@DSLValidation
	public void validateUserInterfaceObjectIsWellDefined(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getUserInterface() != null) {
			// Validate metric list
			final List<MetricGroup> metricGroups = this.entity.getUserInterface().getMetricGroups();
			for (final MetricGroup metricGroup : metricGroups) {
				for (final Object metric : metricGroup.getMetrics()) {
					if (metric instanceof List<?>) {
						if (!(((List) metric).get(0) instanceof String)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric name should be of type 'String'");
						}
						if (!(((List) metric).get(1) instanceof Unit)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric axisYUnit should be of type org.openspaces.ui.Unit");
						}
						if (!(((List) metric).size() == 2)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric should be defined as String or as a list [String, Unit]");
						}
					} else {
						if (!(metric instanceof String)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric name should be of type 'String'");
						}
					}
				}
			}
		}
	}

	/**
	 * Validates that the name property exists and is not empty or invalid.
	 *
	 * @param validationContext
	 * @throws DSLValidationException
	 */
	@DSLValidation
	public void validateName(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (StringUtils.isBlank(entity.getName())) {
			throw new DSLValidationException("Service.validateName: The service's name "
					+ (entity.getName() == null ? "is missing" : "is empty"));
		}

		DSLUtils.validateRecipeName(entity.getName());
	}

	@DSLValidation
	public void validateInstanceSize(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (entity.getMinAllowedInstances() <= 0) {
			throw new DSLValidationException("Minimum number of instances ("
					+ this.entity.getMinAllowedInstances()
					+ ") must be 1 or higher.");
		}
		if (this.entity.getMinAllowedInstances() > this.entity.getMaxAllowedInstances()) {
			throw new DSLValidationException(
					"maximum number of instances ("
							+ this.entity.getMaxAllowedInstances()
							+ ") must be equal or greater than the minimum number of instances ("
							+ this.entity.getMinAllowedInstances() + ")");
		}

		if (this.entity.getMinAllowedInstances() > this.entity.getNumInstances()) {
			throw new DSLValidationException(
					"number of instances ("
							+ this.entity.getNumInstances()
							+ ") must be equal or greater than the minimum number of instances ("
							+ this.entity.getMinAllowedInstances() + ")");
		}
		if (this.entity.getNumInstances() > this.entity.getMaxAllowedInstances()) {
			throw new DSLValidationException(
					"number of instances ("
							+ this.entity.getNumInstances()
							+ ") must be equal or less than the maximum number of instances ("
							+ this.entity.getMaxAllowedInstances() + ")");
		}
		if (this.entity.getNumInstances() <= 0) {
			throw new DSLValidationException(
					"number of instances must be set to a positive integer");
		}
	}

	@DSLValidation
	public void validateScalingRules(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getScalingRules() != null) {
			for (final ScalingRuleDetails scalingRule : this.entity.getScalingRules()) {
				final Object serviceStatisticsObject = scalingRule
						.getServiceStatistics();
				if (serviceStatisticsObject == null) {
					throw new DSLValidationException(
							"scalingRule must specify serviceStatistics (either a closure or "
									+ "reference a predefined serviceStatistics name).");
				}
			}
		}
	}

}
