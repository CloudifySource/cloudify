/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.domain.network;

import java.util.LinkedList;
import java.util.List;

/********
 * Represents a port range, which can contains multiple sub ranges.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class PortRange {

	private List<PortRangeEntry> ranges = new LinkedList<PortRangeEntry>();

	public List<PortRangeEntry> getRanges() {
		return ranges;
	}

	public void setRanges(final List<PortRangeEntry> ranges) {
		this.ranges = ranges;
	}
}
