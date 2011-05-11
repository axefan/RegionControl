package us.axefan.plugin;

import java.util.ArrayList;

public class ArrayUtils {
	
	public static String[] trim(String[] source){
		ArrayList<String> retval = new ArrayList<String>();
		for (int i=0; i<source.length; i++){
			if (source[i].trim().length() > 0){
				retval.add(source[i]);
			}
		}
		return retval.toArray(new String[retval.size()]);
	}
	
	public static String join(String[] source, String delim){
		if (source == null) return "";
		if (source.length == 0) return "";
		StringBuilder retval = new StringBuilder();
		retval.append(source[0]);
		for (int i=1; i<source.length; i++){
			retval.append(delim);
			retval.append(source[i]);
		}
		return retval.toString();
	}
	
}
