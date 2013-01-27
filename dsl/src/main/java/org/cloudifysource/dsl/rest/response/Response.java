package org.cloudifysource.dsl.rest.response;

/**
 * 
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
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public String getVerbose() {
		return verbose;
	}
	
	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}
	
	public T getResponse() {
		return response;
	}
	
	public void setResponse(T response) {
		this.response = response;
	}
}
