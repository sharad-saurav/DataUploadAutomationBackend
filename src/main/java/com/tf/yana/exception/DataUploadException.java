package com.tf.yana.exception;

public class DataUploadException extends Exception {

	public DataUploadException(String message) {
		super(message);
		System.out.println(message);
	}

}
