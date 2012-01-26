package org.cloudifysource.usm.launcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyExceptionHandler {
	
	private static final String RUNTIME_EXCEPTION_CAUGHT_REGEX = "(Caught:.*\\.groovy:[1-9]{1,}\\))";
	private static final String COMPILATION_EXCEPTION_CAUGHT_REGEX = "(([a-zA-Z]*:\\\\|/).+\\.groovy:\\s[1-9]+.*column\\s[1-9]+)";
	
	/**
	 * 
	 * Extracts the groovy exception string from a given string.
	 * Currently supports groovy Runtime and Compilation exceptions. 
	 * @param input The string containing the groovy exception.
	 * @return Groovy exception string. empty string if not found.
	 */
	public static String getExceptionString(String input){
		String exceptionReason = getRuntimeException(input);
		if (exceptionReason.length() == 0){
			//No runtime exception was found. look for a compilation exception.
			exceptionReason = getCompilationException(input);
		}
		return exceptionReason;
	}
	
	/**
	 * returns the groovy runtime exception from a given string
	 * @param input
	 * @return The Runtime exception string, empty string if not found.
	 */
	private static String getRuntimeException(String input){
		return getPatternMatch(input, RUNTIME_EXCEPTION_CAUGHT_REGEX, Pattern.MULTILINE + Pattern.DOTALL);
	}
	
	/**
	 * returns the groovy compilation exception from a given string
	 * @param input
	 * @return The compilation exception string, empty string if not found.
	 */
	private static String getCompilationException(String input){
		return getPatternMatch(input, COMPILATION_EXCEPTION_CAUGHT_REGEX, Pattern.MULTILINE);
	}
	
	private static String getPatternMatch(String input, String regex, int regexFlags){
		
		if (input == null) {
			return "";
		}
		//create the pattern using the regex and flags.
		//this pattern will only be used in cases where a groovy exception occurs
		//So creating the pattern will be done locally.
		Pattern pattern = Pattern.compile(regex, regexFlags);
		Matcher matcher = pattern.matcher(input);
		int beginIndex = 0;
		int endIndex = 0;
		if (matcher.find()) {
			beginIndex = matcher.start(0);
			endIndex = matcher.end(0);
		}
		return input.substring(beginIndex, endIndex);
	}
}
