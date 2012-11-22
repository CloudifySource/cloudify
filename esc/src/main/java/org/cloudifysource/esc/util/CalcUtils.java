package org.cloudifysource.esc.util;

import java.util.concurrent.TimeoutException;

/**
 * A utility class for calculations.
 * 
 * @author noak
 * @since 2.3.1
 */
public class CalcUtils {
	/**
	 * Calculates the milliseconds remaining until the given end time.
	 * 
	 * @param end The end time, in milliseconds
	 * @return Number of milliseconds remaining until the given end time
	 * @throws TimeoutException Thrown when the end time is in the past
	 */
	public static long millisUntil(final long end)
			throws TimeoutException {
		final long millisUntilEnd = end - System.currentTimeMillis();
		if (millisUntilEnd < 0) {
			throw new TimeoutException("Cloud operation timed out");
		}
		return millisUntilEnd;
	}

	/**
	 * Safely casts long to int.
	 * 
	 * @param longValue The long to cast
	 * @param roundIfNeeded Indicating whether to change the value of the number if it exceeds int's max/min values. If
	 *        set to false and the long is too large/small, an {@link IllegalArgumentException} is thrown.
	 * @return int representing of the given long.
	 */
	public static int safeLongToInt(final long longValue, final boolean roundIfNeeded) {
		int intValue;
		if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
			if (roundIfNeeded) {
				if (longValue < Integer.MIN_VALUE) {
					intValue = Integer.MIN_VALUE;
				} else {
					intValue = Integer.MAX_VALUE;
				}
			} else {
				throw new IllegalArgumentException(longValue + " cannot be cast to int without changing its value.");
			}
		} else {
			intValue = (int) longValue;
		}
		return intValue;
	}


}
