/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.jmx;

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
		if (targetStr == null || targetStr.isEmpty()) {
			return null;
		}
		StringTokenizer tok2 = new StringTokenizer(targetStr, ":");
		int ctr = 0;
		String domain = "", type = "", attr = "", dispName = "";
				
		while (tok2.hasMoreTokens()) {
			String elem = tok2.nextToken();
			if (elem.isEmpty()) {
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
