package com.langpack.common;

public class InvalidRequestException extends Exception {
	private static final long serialVersionUID = 99999991L;

	public InvalidRequestException(String message) {
		super(message);
	}
}