package com.gigaspaces.cloudify.usm.jmx;

import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A parser which accepts a user-defined string and returns a JmxTarget
 * 
 * @author giladh
 * @since 8.0.1
 *
 */
public class JmxTargetParser {

	private static final Log logger = LogFactory.getLog(JmxTargetParser.class);

	
	public static JmxTarget parse(String targetStr) {
		if (targetStr == null || targetStr.length() == 0) {
			return null;
		}
		StringTokenizer tok2 = new StringTokenizer(targetStr, ":");
		int ctr = 0;
		String domain = "", type = "", attr = "", dispName = "";
		String castTo = null;
				
		while (tok2.hasMoreTokens()) {
			String elem = tok2.nextToken();
			if (elem.length() == 0) {
				continue;
			}
			ctr++;
			if (ctr == 1) {
				domain = elem;						
			} 
			else if (ctr == 2) {
				type = elem;						
			} 
			else if (ctr == 3) {
				attr = elem;
				dispName = attr; // is attr name as default 
			}
			else if (ctr == 4) {
				castTo = elem;
			}
		}
		
		if (ctr < 3 || ctr > 4) {
			logger.error("Failed to parse JMX target string: " + targetStr);
			return null;				
		}
		logger.info("JMX target: domain=" + domain + ", type=" + type + ", attr=" + attr + ", dispName=" + dispName);
		return new JmxTarget(domain, type, attr, dispName);		
	}						
	
	// non instantiable
	private JmxTargetParser() {}
}
