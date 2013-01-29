package org.cloudifysource.dsl.rest.response;

/**
 * A POJO representing a generic response from the REST Gateway.
 * @author elip
 *
 * @param <T>
 */
public class Response<T> {

	private String status;
	private String message;
	private String messageId;
	private String verbose;
	private T response;
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(final String status) {
		this.status = status;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(final String messageId) {
		this.messageId = messageId;
	}
	
	public String getVerbose() {
		return verbose;
	}
	
	public void setVerbose(final String verbose) {
		this.verbose = verbose;
	}
	
	public T getResponse() {
		return response;
	}
	
	public void setResponse(final T response) {
		this.response = response;
	}
}
