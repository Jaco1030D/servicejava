package com.magmatranslation.xliffconverter.services.storage;

public class StorageFileNotFoundException extends StorageException {
    public StorageFileNotFoundException(String message) {
		super(message);
	}

	public StorageFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
