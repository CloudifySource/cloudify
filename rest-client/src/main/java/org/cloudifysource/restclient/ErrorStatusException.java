package org.cloudifysource.restclient;

/**
 * This exception a detailed error status, extending
 * {@link org.cloudifysource.restclient.RestException}. The reasonCode and
 * args can be used to created formatted messages from the message bundle.
 * 
 * @author uri
 */
public class ErrorStatusException extends RestException {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = -399277091070772297L;

	/**
	 * reason code constant text.
	 */
	private static final String REASON_CODE = "reasonCode";

	/**
	 * unique reason code describing this exception. Matches a formatted message
	 * in the messages bundle.
	 */
	private String reasonCode;

	/**
	 * an array of possible arguments to complete the reason code.
	 */
	private Object[] args;

	/**
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 * @param reasonCode
	 *            A unique code used to retrieve the formatted message from a
	 *            message bundle
	 * @param args
	 *            Optional arguments combined in the message
	 */
	public ErrorStatusException(final Throwable cause, final String reasonCode,
			final Object... args) {
		super(REASON_CODE + reasonCode, cause);
		this.args = args;
		this.reasonCode = reasonCode;
	}

	/**
	 * @param reasonCode
	 *            A unique code used to retrieve the formatted message from a
	 *            message bundle
	 * @param args
	 *            Optional arguments combined in the message
	 */
	public ErrorStatusException(final String reasonCode, final Object... args) {
		super(REASON_CODE + reasonCode);
		this.reasonCode = reasonCode;
		this.args = args;
	}

	/**
	 * Gets the reason code.
	 * 
	 * @return reason code as String
	 */
	public final String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets the arguments array.
	 * 
	 * @return arguments as Object[]
	 */
	public final Object[] getArgs() {
		return args;
	}
}