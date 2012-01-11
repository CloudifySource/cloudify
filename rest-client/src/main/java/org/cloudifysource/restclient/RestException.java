package org.cloudifysource.restclient;

/**
 * Exception representing a failure in the communication through rest. For More
 * detailed exceptions use
 * {@link org.cloudifysource.restclient.ErrorStatusException}
 */
public class RestException extends Exception {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = -7304916239245226345L;

	/**
	 * Empty Ctor.
	 */
	public RestException() {
	}

	/**
	 * @param message
	 *            A String error message describing this exception
	 */
	public RestException(final String message) {
		super(message);

	}

	/**
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 */
	public RestException(final Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 *            A String error message describing this exception
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 */
	public RestException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
