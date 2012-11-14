package org.cloudifysource.dsl.internal;

/**********
 * Enum for cloufify error messages, including keys to message bundle.
 * @author barakme
 *
 */
public enum CloudifyErrorMessages {

	/******
	 * If service recipe refers to missing template.
	 */
	MISSING_TEMPLATE("missing_template", 1),
	
	/**
	 * Is the cloud overrides given is to long. the file size limit is 10K.
	 */
	CLOUD_OVERRIDES_TO_LONG("cloud_overrides_file_to_long", 0);
	
	private final int numberOfParameters;
	private final String name;
	
	CloudifyErrorMessages(final String name, final int numberOfParameters) {
		this.name = name;
		this.numberOfParameters = numberOfParameters;
	}

	public int getNumberOfParameters() {
		return numberOfParameters;
	}

	public String getName() {
		return name;
	}
	
	
}
