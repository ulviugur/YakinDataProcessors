package com.langpack.common;

public class ReflectionUtils {
	// remove the method name from the arguments
	public static String[] removeFirstElement(String[] args) {
		String[] retval = new String[] {};
		if (args == null) {
			retval = args;
		} else if (args.length == 1) {
			retval = retval;
		} else {
			retval = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				retval[i - 1] = args[i];
			}
		}
		return retval;
	}

	public static Class[] getParameterClassArray(String[] params) {
		Class[] retval = new Class[] {};
		if (params == null) {
			retval = null;
		} else if (params.length == 0) {
			retval = retval;
		} else {
			retval = new Class[params.length];
			for (int i = 0; i < params.length; i++) {
				retval[i] = String.class;
			}
		}
		return retval;
	}
}
