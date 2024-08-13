package com.langpack.structure;

public class Helper {

	public static boolean checkInArray(String toCheckValue, String[] arr) {
		// check if the specified element
		// is present in the array or not
		// using Linear Search method
		boolean test = false;
		if (toCheckValue == null) {
			return test;
		}

		for (String element : arr) {
			if (element.equals(toCheckValue)) {
				test = true;
				break;
			}
		}
		return test;
	}
}
