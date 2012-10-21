/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.restDoclet.docElements;

import org.cloudifysource.restDoclet.constants.RestDocConstants;

public class DocPossibleResponseStatusesAnnotation extends DocAnnotation {

	private Integer[] codes;
	private String[] descriptions;

	public DocPossibleResponseStatusesAnnotation(String name) {
		super(name);
	}

	/**
	 * @return the codes
	 */
	public Integer[] getCodes() {
		return codes;
	}

	/**
	 * @return the descriptions
	 */
	public String[] getDescriptions() {
		return this.descriptions;
	}

	@Override
	public void addAttribute(String attrName, Object attrValue) {
		String shortAttrName = getShortName(attrName);

		if(shortAttrName.endsWith(RestDocConstants.POSSIBLE_RESPONSE_STATUSES_CODES)) {
			codes = (Integer[])attrValue;
		}
		else if(shortAttrName.endsWith(RestDocConstants.POSSIBLE_RESPONSE_STATUSES_DESCRIPTIONS)) {
			descriptions = (String[])attrValue;
		}

		super.addAttribute(shortAttrName, attrValue);
	}

	@Override
	public String toString() {
		String str = "@" + RestDocConstants.POSSIBLE_RESPONSE_STATUSES_ANNOTATION + "[";
		if(codes != null) {
			str += "codes: {";
			StringBuilder codesStr = new StringBuilder();
			for (int i = 0; i < codes.length; i++) {		
				if(i != 0)
					codesStr.append(", ");
				codesStr.append(codes[i]);
			}
			str += codesStr + "}";
		}
		if(descriptions != null) {
			str += " descriptions: {";
			StringBuilder descriptionStr = new StringBuilder();
			for (int i = 0; i < descriptions.length; i++) {
				if(i != 0)
					descriptionStr.append(", ");
				descriptionStr.append(descriptions[i]);
			}
			str += descriptionStr + "}";
		}
		return str + "]";
	}

}
