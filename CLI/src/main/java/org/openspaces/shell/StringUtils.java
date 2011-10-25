package org.openspaces.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author rafi
 * @since 8.0.3
 */
public class StringUtils {

	public static boolean notEmpty(String s) {
		return s != null && s.length() > 0;
	}

	public static String getStringFromStream(InputStream is) throws IOException{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while((line = bufferedReader.readLine()) != null){
			sb.append(line);
		}
		return sb.toString();
	}

}
