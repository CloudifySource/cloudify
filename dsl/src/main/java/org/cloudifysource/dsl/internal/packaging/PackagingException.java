/**
 * 
 */
package org.cloudifysource.dsl.internal.packaging;

/**
 * @author rafi
 * @since 8.0.3
 */
public class PackagingException extends Exception{

	public PackagingException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public PackagingException(String msg) {
		super(msg);
	}
	
	public PackagingException(Throwable cause) {
		super(cause);
	}
}
