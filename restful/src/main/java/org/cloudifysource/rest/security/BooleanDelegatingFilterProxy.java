package org.cloudifysource.rest.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.DelegatingFilterProxy;

public class BooleanDelegatingFilterProxy extends DelegatingFilterProxy{
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
		final String springSecured = System.getProperty("springSecured");

	    if (StringUtils.isNotBlank(springSecured) && springSecured.equalsIgnoreCase("true")) {
	    	// Call the delegate
		      super.doFilter(request, response, filterChain);
	    } else {
	    	// Ignore the DelegatingProxyFilter delegate
	    	filterChain.doFilter(request, response);
	    }
	  }
}
