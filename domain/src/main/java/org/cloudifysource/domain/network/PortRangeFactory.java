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

import java.util.StringTokenizer;

/*****
 * Creates a port range from an input string.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public final class PortRangeFactory {

	private PortRangeFactory() {

	}

	/*************
	 * Creates a port range object from its string description.
	 * 
	 * @param portRangeAsString
	 *            the port range description.
	 * @return the port range object.
	 * @throws IllegalArgumentException if the input string cannot does not match the port range syntax.
	 */
	public static PortRange createPortRange(final String portRangeAsString) throws IllegalArgumentException {
		final StringTokenizer rangeTokenizer = new StringTokenizer(portRangeAsString, ",");
		int counter = 1;
		final PortRange result = new PortRange();
		while (rangeTokenizer.hasMoreElements()) {
			final String currentRangeString = rangeTokenizer.nextToken().trim();
			handleCurrentRange(portRangeAsString, counter, result, currentRangeString);

			++counter;
		}

		return result;
	}

	private static void handleCurrentRange(final String portRangeAsString, final int counter, final PortRange result,
			final String currentRangeString) {
		final StringTokenizer entryTokenizer = new StringTokenizer(currentRangeString, "-");
		final String fromAsString = entryTokenizer.nextToken().trim();
		String toAsString = null;
		if (entryTokenizer.hasMoreElements()) {
			toAsString = entryTokenizer.nextToken().trim();
		}
		
		if (entryTokenizer.hasMoreElements()) {
			throw new IllegalArgumentException("Illegal port range entry in entry number: " + counter
					+ " of port range expression: " + portRangeAsString);
		}

		try {
			final Integer from = Integer.parseInt(fromAsString);
			Integer to = null;
			if (toAsString != null) {
				to = Integer.parseInt(toAsString);
			}
			final PortRangeEntry entry = new PortRangeEntry();
			entry.setFrom(from);
			entry.setTo(to);
			result.getRanges().add(entry);
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Failed to parse port values from range expression: "
					+ currentRangeString, e);
		}
	}

}
