package org.cloudifysource.restDoclet.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 
 * Annotation to specify an example of a JSON response.
 * Should be use within the REST's controllers above requestMapping methods 
 * to specify an example that will be shown in the REST API documentation. 
 * @author yael
 *
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonResponseExample {
	
	/**
	 * 
	 * The status.
	 * 
	 */
	String status();
	
	/**
	 * 
	 * The response.
	 */
	String responseBody() default "{}";
	
	/**
	 * 
	 * Comments to describe the response's body.
	 * 
	 */
	String comments() default "";
}
